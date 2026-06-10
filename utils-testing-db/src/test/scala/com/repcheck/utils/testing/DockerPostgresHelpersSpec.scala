package com.repcheck.utils.testing

import java.util.concurrent.atomic.AtomicBoolean

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class DockerPostgresHelpersSpec extends AnyFlatSpec with Matchers {

  "resolveDockerBin" should "prefer the DOCKER_BIN override" in {
    DockerPostgres.resolveDockerBin(Some("Windows 11"), Some("/custom/docker")) shouldBe "/custom/docker"
  }

  it should "use the Docker Desktop path on Windows" in {
    DockerPostgres.resolveDockerBin(Some("Windows 11"), None) should include("docker.exe")
  }

  it should "use bare docker elsewhere" in {
    DockerPostgres.resolveDockerBin(Some("Linux"), None) shouldBe "docker"
  }

  it should "use bare docker when the OS is unknown" in {
    DockerPostgres.resolveDockerBin(None, None) shouldBe "docker"
  }

  "parseHostPort" should "take the port after the last colon" in {
    DockerPostgres.parseHostPort("0.0.0.0:54321\n") shouldBe 54321
  }

  "requireContainerStartSucceeded" should "pass on exit code 0 and fail otherwise" in {
    DockerPostgres.requireContainerStartSucceeded(0)
    val ex = intercept[RuntimeException](DockerPostgres.requireContainerStartSucceeded(125))
    ex.getMessage should include("Is Docker running?")
  }

  "failOnReadinessExhaustion" should "clean up, then fail with the attempt budget" in {
    val cleaned = new AtomicBoolean(false)
    val ex = intercept[RuntimeException](
      DockerPostgres.failOnReadinessExhaustion(() => cleaned.set(true), maxAttempts = 7)
    )
    val _ = cleaned.get() shouldBe true
    ex.getMessage should include("after 7 attempts")
  }

  "waitForReady" should "retry on a missing container, then clean up and fail when exhausted" in {
    val fixture = new DockerPostgres(PostgresContainerConfig(readyDelayMs = 1L))
    val ex      = intercept[RuntimeException](fixture.waitForReady("utils-testing-ghost", remaining = 2))
    ex.getMessage should include("did not become ready")
  }

  "connectWithRetry" should "exhaust its budget against a closed port" in {
    val fixture =
      new DockerPostgres(PostgresContainerConfig(maxConnectAttempts = 2, connectDelayMs = 1L))
    val ex = intercept[RuntimeException](fixture.connectWithRetry(port = 1, remaining = 2))
    ex.getMessage should include("after 2 attempts")
  }

  "PostgresContainerConfig" should "default to a vanilla postgres image with a no-op schema hook" in {
    val config = PostgresContainerConfig()
    val _      = config.image shouldBe "postgres:16-alpine"
    val _      = config.dbName shouldBe "test"
    val _      = config.user shouldBe "test"
    val _      = config.password shouldBe "test"
    config.maxReadyAttempts shouldBe 120
  }

}
