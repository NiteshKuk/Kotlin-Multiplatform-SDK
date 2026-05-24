package com.kmpsdk.`data`.db

import kotlin.Long
import kotlin.String

public data class Offline_queue(
  public val id: Long,
  public val method: String,
  public val url: String,
  public val headers_json: String,
  public val body: String?,
  public val priority: Long,
  public val created_at: Long,
  public val retry_count: Long,
  public val max_retries: Long,
  public val status: String,
)
