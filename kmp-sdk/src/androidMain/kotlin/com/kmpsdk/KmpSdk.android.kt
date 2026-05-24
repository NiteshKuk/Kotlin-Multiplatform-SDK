package com.kmpsdk

import android.content.Context

/**
 * Android one-call init: platform context + SDK config + feature modules.
 */
fun KmpSdk.init(
    context: Context,
    block: KmpSdkInitBuilder.() -> Unit,
) {
    KmpSdkAndroid.init(context)
    val builder = KmpSdkInitBuilder().apply(block)
    init(
        config = builder.buildConfig(),
        tokenRefreshHandler = builder.tokenRefreshHandler,
    ) {
        builder.modules().forEach(::install)
    }
}
