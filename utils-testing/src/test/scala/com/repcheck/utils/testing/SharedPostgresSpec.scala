package com.repcheck.utils.testing

import scala.util.Using

import cats.effect.IO

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import com.repcheck.utils.tags.DockerRequired

object SharedSpecDb extends SharedPostgres(PostgresContainerConfig(containerNamePrefix = "utils-testing-shared"))

class SharedPostgresSpec extends AnyFlatSpec with Matchers {

  "SharedPostgres" should "serve one live container to every caller" taggedAs DockerRequired in {
    val first  = SharedSpecDb.info
    val second = SharedSpecDb.info
    val _      = second shouldBe first
    Using.resource(first.getConnection) { conn =>
      Using.resource(conn.createStatement()) { stmt =>
        val rs = stmt.executeQuery("SELECT 1")
        val _  = rs.next()
        rs.getInt(1) shouldBe 1
      }
    }
  }

  "runFinalizer" should "swallow finalizer failures" in {
    SharedPostgres.runFinalizer(IO.raiseError(new RuntimeException("already gone")))
    SharedPostgres.runFinalizer(IO.unit)
    succeed
  }

  "releaseOnJvmExit" should "register a hook whose body runs the finalizer" in {
    val hook = SharedPostgres.releaseOnJvmExit(IO.unit)
    hook.run()
    val _ = Runtime.getRuntime.removeShutdownHook(hook) shouldBe true
    succeed
  }

}
