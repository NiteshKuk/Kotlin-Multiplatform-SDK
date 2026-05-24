package com.kmpsdk.`data`.db

import kotlin.Long
import kotlin.String

public data class Api_cache(
  public val cache_key: String,
  public val response_body: String,
  public val created_at: Long,
  public val ttl_millis: Long,
)
