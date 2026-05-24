package com.kmpsdk.data.local

import com.kmpsdk.data.source.LocalListDataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Generic SQLDelight-backed list store — host apps provide query lambdas only.
 */
class SqlDelightListLocalDataSource<TDomain, TRow>(
    private val observeRows: () -> Flow<List<TRow>>,
    private val toDomain: (TRow) -> TDomain,
    private val countRows: suspend () -> Long,
    private val replaceRows: suspend (List<TRow>) -> Unit,
) : LocalListDataSource<TDomain> {
    override fun observeAll(): Flow<List<TDomain>> =
        observeRows().map { rows -> rows.map(toDomain) }

    override suspend fun count(): Long = countRows()

    suspend fun replaceAll(rows: List<TRow>) = withContext(Dispatchers.Default) {
        replaceRows(rows)
    }
}

/**
 * Paginated SQLDelight store with replace-all and append-page helpers.
 */
class SqlDelightPaginatedLocalDataSource<TDomain, TRow>(
    private val observeRows: () -> Flow<List<TRow>>,
    private val toDomain: (TRow) -> TDomain,
    private val countRows: suspend () -> Long,
    private val replaceRows: suspend (List<TRow>) -> Unit,
    private val appendRows: suspend (List<TRow>) -> Unit,
) {
    fun observeAll(): Flow<List<TDomain>> =
        observeRows().map { rows -> rows.map(toDomain) }

    suspend fun count(): Long = countRows()

    suspend fun replaceAll(rows: List<TRow>) = withContext(Dispatchers.Default) {
        replaceRows(rows)
    }

    suspend fun appendAll(rows: List<TRow>) = withContext(Dispatchers.Default) {
        appendRows(rows)
    }
}
