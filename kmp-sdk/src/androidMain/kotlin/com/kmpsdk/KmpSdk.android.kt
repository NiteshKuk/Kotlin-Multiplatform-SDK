package com.kmpsdk

import android.content.Context

/**
 * Android one-call init: platform context + SDK config + feature modules.
 * Delegates to common [KmpSdk.init] so deep links, push, env vault, remote config,
 * background work, and host [KmpSdkInitBuilder.register] apply correctly.
 */
fun KmpSdk.init(
    context: Context,
    block: KmpSdkInitBuilder.() -> Unit,
) {
    KmpSdkAndroid.init(context)
    init(block)
}
