package com.repcheck.utils.testing

import java.sql.{Connection, DriverManager}

final case class PostgresContainerInfo(jdbcUrl: String, user: String, password: String) {

  def getConnection: Connection =
    DriverManager.getConnection(jdbcUrl, user, password)

}
