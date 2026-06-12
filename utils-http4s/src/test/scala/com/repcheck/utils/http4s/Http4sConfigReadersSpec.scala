package com.repcheck.utils.http4s

import pureconfig.{ConfigReader, ConfigSource}

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class Http4sConfigReadersSpec extends AnyFlatSpec with Matchers {

  import Http4sConfigReaders.given

  final private case class Endpoint(baseUri: org.http4s.Uri) derives ConfigReader

  "the Uri reader" should "parse a valid URI" in {
    ConfigSource.string("""{ base-uri = "http://localhost:11434" }""").load[Endpoint] match {
      case Right(endpoint) => endpoint.baseUri.renderString shouldBe "http://localhost:11434"
      case Left(failures)  => fail(s"expected a loaded config, got $failures")
    }
  }

  it should "reject a malformed URI with a CannotConvert failure" in {
    ConfigSource.string("""{ base-uri = "http://exa mple.com" }""").load[Endpoint] match {
      case Left(failures) => failures.toList.mkString should include("Uri")
      case Right(value)   => fail(s"expected a load failure, got $value")
    }
  }

}
