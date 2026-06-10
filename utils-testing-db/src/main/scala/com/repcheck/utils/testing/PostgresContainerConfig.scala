package com.repcheck.utils.testing

import java.sql.Connection

/**
 * @param initSchema
 *   applied once after the container accepts connections — pass a migration runner here (e.g. Liquibase) so the fixture
 *   itself stays schema-agnostic.
 */
final case class PostgresContainerConfig(
  image: String = "postgres:16-alpine",
  dbName: String = "test",
  user: String = "test",
  password: String = "test",
  containerNamePrefix: String = "repcheck-test",
  initSchema: Connection => Unit = _ => (),
  maxReadyAttempts: Int = 120,
  readyDelayMs: Long = 1000L,
  maxConnectAttempts: Int = 60,
  connectDelayMs: Long = 1000L,
)
