package com.repcheck.utils.testing

import java.sql.{Connection, DriverManager}

import scala.annotation.tailrec
import scala.sys.process._
import scala.util.Try

import cats.effect.{IO, Resource}

/**
 * Docker-backed Postgres test fixture, schema-agnostic: the container family (image, db, credentials, readiness
 * budgets) and schema initialisation come from [[PostgresContainerConfig]], so any repo can run its migrations via the
 * `initSchema` hook. Generalised from data-ingestion's `common-testing` copy, which hard-wired the RepCheck Liquibase
 * changelog.
 */
final class DockerPostgres(config: PostgresContainerConfig) {

  import DockerPostgres._

  val resource: Resource[IO, PostgresContainerInfo] =
    Resource.make(acquire)(release).map(_.info)

  final private case class ContainerHandle(name: String, info: PostgresContainerInfo)

  private def acquire: IO[ContainerHandle] = IO.blocking {
    val containerName = s"${config.containerNamePrefix}-${java.util.UUID.randomUUID().toString.take(8)}"
    val port          = startContainer(containerName)
    waitForReady(containerName)
    applySchema(port)
    ContainerHandle(
      name = containerName,
      info = PostgresContainerInfo(
        jdbcUrl = jdbcUrl(port),
        user = config.user,
        password = config.password,
      ),
    )
  }

  private def release(handle: ContainerHandle): IO[Unit] = IO.blocking {
    val _ = Seq(dockerBin, "rm", "-f", handle.name).!
    ()
  }

  private def jdbcUrl(port: Int): String =
    s"jdbc:postgresql://localhost:${port.toString}/${config.dbName}?sslmode=disable"

  private def startContainer(containerName: String): Int = {
    val exitCode = Seq(
      dockerBin,
      "run",
      "-d",
      "--name",
      containerName,
      "-e",
      s"POSTGRES_DB=${config.dbName}",
      "-e",
      s"POSTGRES_USER=${config.user}",
      "-e",
      s"POSTGRES_PASSWORD=${config.password}",
      "-p",
      "0:5432",
      config.image,
    ).!

    requireContainerStartSucceeded(exitCode)

    parseHostPort(Seq(dockerBin, "port", containerName, "5432").!!)
  }

  @tailrec
  private[testing] def waitForReady(containerName: String, remaining: Int = config.maxReadyAttempts): Unit = {
    if (remaining <= 0) {
      failOnReadinessExhaustion(
        cleanup = () => { val _ = Seq(dockerBin, "rm", "-f", containerName).!; () },
        maxAttempts = config.maxReadyAttempts,
      )
    }

    val ready = Try {
      Seq(dockerBin, "exec", containerName, "pg_isready", "-U", config.user, "-d", config.dbName).!!
    }.isSuccess

    if (!ready) {
      Thread.sleep(config.readyDelayMs)
      waitForReady(containerName, remaining - 1)
    }
  }

  private def applySchema(port: Int): Unit = {
    val conn = connectWithRetry(port, config.maxConnectAttempts)
    try config.initSchema(conn)
    finally conn.close()
  }

  @tailrec
  private[testing] def connectWithRetry(port: Int, remaining: Int): Connection = {
    val result = Try {
      DriverManager.getConnection(jdbcUrl(port), config.user, config.password)
    }
    result match {
      case scala.util.Success(conn) => conn
      case scala.util.Failure(_) if remaining > 1 =>
        Thread.sleep(config.connectDelayMs)
        connectWithRetry(port, remaining - 1)
      case scala.util.Failure(ex) =>
        sys.error(
          s"Failed to connect to PostgreSQL after ${config.maxConnectAttempts.toString} attempts: ${ex.getMessage}"
        )
    }
  }

}

object DockerPostgres {

  // Windows: ProcessBuilder does not resolve PATHEXT and the sbt-forked JVM's PATH may lack the docker shim — use the
  // Docker Desktop absolute path unless DOCKER_BIN overrides; Linux/macOS keep bare `docker`.
  private[testing] val dockerBin: String =
    resolveDockerBin(sys.props.get("os.name"), sys.env.get("DOCKER_BIN"))

  private[testing] def resolveDockerBin(osName: Option[String], envOverride: Option[String]): String =
    envOverride.getOrElse {
      val isWindows = osName.exists(_.toLowerCase.contains("windows"))
      if (isWindows) """C:\Program Files\Docker\Docker\resources\bin\docker.exe""" else "docker"
    }

  private[testing] def parseHostPort(portOutput: String): Int = {
    val trimmed = portOutput.trim
    trimmed.substring(trimmed.lastIndexOf(':') + 1).toInt
  }

  private[testing] def requireContainerStartSucceeded(exitCode: Int): Unit =
    if (exitCode != 0) {
      sys.error("Failed to start Docker container. Is Docker running?")
    }

  private[testing] def failOnReadinessExhaustion(cleanup: () => Unit, maxAttempts: Int): Nothing = {
    cleanup()
    sys.error(s"PostgreSQL container did not become ready after ${maxAttempts.toString} attempts")
  }

}
