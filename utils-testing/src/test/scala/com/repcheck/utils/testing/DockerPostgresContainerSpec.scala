package com.repcheck.utils.testing

import scala.util.Using

import cats.effect.IO
import cats.effect.testing.scalatest.AsyncIOSpec

import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers

import com.repcheck.utils.tags.DockerRequired

/** Runs a REAL container (in CI too) — this is how the fixture's container paths meet the coverage gate. */
class DockerPostgresContainerSpec extends AsyncFlatSpec with AsyncIOSpec with Matchers {

  private val config = PostgresContainerConfig(
    containerNamePrefix = "utils-testing-spec",
    initSchema = conn =>
      Using.resource(conn.createStatement()) { stmt =>
        val _ = stmt.execute("CREATE TABLE fixture_proof(id INT)")
      },
  )

  "DockerPostgres.resource" should "start postgres, apply the schema hook, and connect" taggedAs DockerRequired in {
    new DockerPostgres(config).resource
      .use { info =>
        IO.blocking {
          Using.resource(info.getConnection) { conn =>
            Using.resource(conn.createStatement()) { stmt =>
              val _  = stmt.execute("INSERT INTO fixture_proof VALUES (1)")
              val rs = stmt.executeQuery("SELECT COUNT(*) FROM fixture_proof")
              val _  = rs.next()
              rs.getInt(1)
            }
          }
        }
      }
      .asserting(_ shouldBe 1)
  }

}
