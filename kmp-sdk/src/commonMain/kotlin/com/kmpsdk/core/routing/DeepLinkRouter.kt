package com.kmpsdk.core.routing

import com.kmpsdk.core.logger.Logger

typealias DeepLinkHandler = suspend (args: Map<String, String>, rawUrl: String) -> Unit

data class DeepLinkRoute(
    val pattern: String,
    val handler: DeepLinkHandler,
)

/**
 * Shared deep-link patterns → SDK/host actions.
 * Platform code only forwards the URL string into [handle].
 */
class DeepLinkRouter(
    private val logger: Logger = Logger.create("DeepLinks"),
) {
    private val routes = mutableListOf<DeepLinkRoute>()

    fun route(pattern: String, handler: DeepLinkHandler) {
        routes.removeAll { it.pattern == pattern }
        routes += DeepLinkRoute(pattern, handler)
    }

    fun routes(block: DeepLinkRouter.() -> Unit) = apply(block)

    /**
     * @return true if a route matched
     */
    suspend fun handle(url: String): Boolean {
        val path = normalize(url)
        for (route in routes) {
            val args = match(route.pattern, path) ?: continue
            logger.i("Deep link matched '${route.pattern}' ← $url")
            route.handler(args, url)
            return true
        }
        logger.w("No deep link route for $url")
        return false
    }

    private fun normalize(url: String): String {
        val withoutScheme = url.substringAfter("://", url)
        val path = withoutScheme.substringAfter('/', withoutScheme).substringBefore('?')
        return path.trim('/')
    }

    private fun match(pattern: String, path: String): Map<String, String>? {
        val patternParts = pattern.trim('/').split('/')
        val pathParts = path.trim('/').split('/')
        if (patternParts.size != pathParts.size) return null
        val args = mutableMapOf<String, String>()
        for (i in patternParts.indices) {
            val p = patternParts[i]
            val v = pathParts[i]
            if (p.startsWith("{") && p.endsWith("}")) {
                args[p.removePrefix("{").removeSuffix("}")] = v
            } else if (p != v) {
                return null
            }
        }
        return args
    }
}
