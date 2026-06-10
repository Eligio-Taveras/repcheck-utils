package com.repcheck.utils.errors

import pureconfig.ConfigReader

final case class RetryConfig(
  maxRetries: Int = 3,
  initialBackoffMs: Long = 10L,
  maxBackoffMs: Long = 60000L,
  backoffMultiplier: Double = 2.0,
) derives ConfigReader
