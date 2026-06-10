package com.repcheck.utils.codecs

import java.time.{LocalDate, ZonedDateTime}
import java.util.UUID

import io.circe.Json
import io.circe.syntax._

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import com.repcheck.utils.codecs.DateTimeCodecs._

class DateTimeCodecsSpec extends AnyFlatSpec with Matchers {

  "ZonedDateTime codec" should "round-trip through ISO format" in {
    val dt = ZonedDateTime.parse("2026-06-10T12:30:00Z")
    dt.asJson.as[ZonedDateTime] shouldBe Right(dt)
  }

  it should "fail to decode a malformed datetime with a named error" in {
    Json.fromString("not-a-datetime").as[ZonedDateTime].left.map(_.getMessage) match {
      case Left(msg) => msg should include("Failed to decode datetime")
      case Right(_)  => fail("expected a decode failure")
    }
  }

  "LocalDate codec" should "round-trip through ISO format" in {
    val d = LocalDate.parse("2026-06-10")
    d.asJson.as[LocalDate] shouldBe Right(d)
  }

  it should "fail to decode a malformed date with a named error" in {
    Json.fromString("06/10/2026").as[LocalDate].left.map(_.getMessage) match {
      case Left(msg) => msg should include("Failed to decode date")
      case Right(_)  => fail("expected a decode failure")
    }
  }

  "UUID codec" should "round-trip" in {
    val id = UUID.fromString("00000000-0000-0000-0000-000000000001")
    id.asJson.as[UUID] shouldBe Right(id)
  }

  it should "fail to decode a malformed UUID with a named error" in {
    Json.fromString("not-a-uuid").as[UUID].left.map(_.getMessage) match {
      case Left(msg) => msg should include("Failed to decode UUID")
      case Right(_)  => fail("expected a decode failure")
    }
  }

}
