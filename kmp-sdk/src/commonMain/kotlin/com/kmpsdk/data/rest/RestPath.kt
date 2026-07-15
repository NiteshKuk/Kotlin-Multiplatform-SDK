package com.kmpsdk.data.rest

/**
 * Resolves REST path templates used by List / Mutation kits.
 *
 * Supports `{id}` and `:id` placeholders.
 */
fun resolveRestPath(template: String, id: String): String =
    template
        .replace("{id}", id)
        .replace(":id", id)

/**
 * Joins a base collection path with an id segment when no template is provided.
 * Example: `/products` + `42` → `/products/42`
 */
fun joinRestPath(basePath: String, id: String): String {
    val trimmed = basePath.trimEnd('/')
    return "$trimmed/$id"
}
