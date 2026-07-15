package com.kmpsdk.data.query

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class SortOrder {
    Asc,
    Desc,
}

internal sealed class QueryPredicate {
    data class Contains(val field: String, val value: String, val ignoreCase: Boolean) : QueryPredicate()
    data class Eq(val field: String, val value: Any?) : QueryPredicate()
    data class LessThan(val field: String, val value: Double) : QueryPredicate()
    data class GreaterThan(val field: String, val value: Double) : QueryPredicate()
}

class FieldClause internal constructor(
    private val field: String,
    private val dsl: QueryDsl<*>,
) {
    fun contains(value: String, ignoreCase: Boolean = true) {
        dsl.add(QueryPredicate.Contains(field, value, ignoreCase))
    }

    fun eq(value: Any?) {
        dsl.add(QueryPredicate.Eq(field, value))
    }

    fun lessThan(value: Number) {
        dsl.add(QueryPredicate.LessThan(field, value.toDouble()))
    }

    fun greaterThan(value: Number) {
        dsl.add(QueryPredicate.GreaterThan(field, value.toDouble()))
    }
}

class QueryDsl<T : Any> {
    internal val predicates = mutableListOf<QueryPredicate>()
    internal var orderField: String? = null
    internal var order: SortOrder = SortOrder.Asc
    internal var limitCount: Int? = null

    fun where(field: String): FieldClause = FieldClause(field, this)

    fun orderBy(field: String, order: SortOrder = SortOrder.Asc) {
        orderField = field
        this.order = order
    }

    fun limit(count: Int) {
        limitCount = count
    }

    internal fun add(predicate: QueryPredicate) {
        predicates += predicate
    }
}

fun interface FieldReader<T> {
    fun read(item: T, field: String): Any?
}

data class QuerySource<T : Any>(
    val name: String,
    val observeAll: () -> Flow<List<T>>,
    val fieldReader: FieldReader<T>,
)

class QueryRegistry {
    private val sources = mutableMapOf<String, QuerySource<*>>()

    fun <T : Any> register(source: QuerySource<T>) {
        sources[source.name] = source
    }

    fun <T : Any> register(
        name: String,
        observeAll: () -> Flow<List<T>>,
        fieldReader: FieldReader<T>,
    ) {
        register(QuerySource(name, observeAll, fieldReader))
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> source(name: String): QuerySource<T> =
        sources[name] as? QuerySource<T>
            ?: error("Query source '$name' is not registered")
}

class QueryKit(
    private val registry: QueryRegistry = QueryRegistry(),
) {
    val sources: QueryRegistry get() = registry

    fun <T : Any> queryFlow(
        name: String,
        block: QueryDsl<T>.() -> Unit,
    ): Flow<List<T>> {
        val source = registry.source<T>(name)
        val dsl = QueryDsl<T>().apply(block)
        return source.observeAll().map { list -> applyQuery(list, dsl, source.fieldReader) }
    }

    fun <T : Any> query(
        name: String,
        items: List<T>,
        fieldReader: FieldReader<T>,
        block: QueryDsl<T>.() -> Unit,
    ): List<T> {
        val dsl = QueryDsl<T>().apply(block)
        return applyQuery(items, dsl, fieldReader)
    }

    private fun <T : Any> applyQuery(
        items: List<T>,
        dsl: QueryDsl<T>,
        reader: FieldReader<T>,
    ): List<T> {
        var result = items.asSequence().filter { item ->
            dsl.predicates.all { predicate -> matches(item, predicate, reader) }
        }
        val orderField = dsl.orderField
        if (orderField != null) {
            result = when (dsl.order) {
                SortOrder.Asc -> result.sortedBy { sortKey(reader.read(it, orderField)) }
                SortOrder.Desc -> result.sortedByDescending { sortKey(reader.read(it, orderField)) }
            }
        }
        return (dsl.limitCount?.let { result.take(it) } ?: result).toList()
    }

    private fun <T : Any> matches(item: T, predicate: QueryPredicate, reader: FieldReader<T>): Boolean {
        return when (predicate) {
            is QueryPredicate.Contains -> {
                val text = reader.read(item, predicate.field)?.toString().orEmpty()
                text.contains(predicate.value, ignoreCase = predicate.ignoreCase)
            }
            is QueryPredicate.Eq -> reader.read(item, predicate.field) == predicate.value
            is QueryPredicate.LessThan -> asDouble(reader.read(item, predicate.field)) < predicate.value
            is QueryPredicate.GreaterThan -> asDouble(reader.read(item, predicate.field)) > predicate.value
        }
    }

    private fun asDouble(value: Any?): Double = when (value) {
        is Number -> value.toDouble()
        is String -> value.toDoubleOrNull() ?: Double.NaN
        else -> Double.NaN
    }

    private fun sortKey(value: Any?): String = when (value) {
        is Number -> value.toDouble().toString().padStart(32, '0')
        else -> value?.toString().orEmpty()
    }
}
