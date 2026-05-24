package com.kmpsdk.`data`.db

import kotlin.Long
import kotlin.String

public data class Offline_action(
  public val id: Long,
  public val action_type: String,
  public val payload_json: String,
  public val created_at: Long,
  public val status: String,
)
