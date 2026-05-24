package com.kmpsdk.core.config

class EnvironmentDsl {
    private val entries = linkedMapOf<String, KmpSdkConfigBuilder.() -> Unit>()

    fun dev(block: KmpSdkConfigBuilder.() -> Unit) = register("dev", block)
    fun staging(block: KmpSdkConfigBuilder.() -> Unit) = register("staging", block)
    fun prod(block: KmpSdkConfigBuilder.() -> Unit) = register("prod", block)
    fun register(name: String, block: KmpSdkConfigBuilder.() -> Unit) {
        entries[name] = block
    }

    internal fun entries(): Map<String, KmpSdkConfigBuilder.() -> Unit> = entries
}

fun buildConfigForEnvironment(
    environmentName: String,
    environments: Map<String, KmpSdkConfigBuilder.() -> Unit>,
    fallback: KmpSdkConfigBuilder.() -> Unit = {},
): KmpSdkConfig {
    val builder = KmpSdkConfigBuilder()
    fallback(builder)
    environments[environmentName]?.invoke(builder)
    return builder.build()
}
