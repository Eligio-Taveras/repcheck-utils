package com.repcheck.utils.errors

import io.circe.parser.decode
import io.circe.syntax._

import pureconfig._

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class ErrorHandlingSpec extends AnyFlatSpec with Matchers {

  // ── ErrorClass Circe round-trip ──

  "ErrorClass" should "encode Transient as string" in {
    val json = (ErrorClass.Transient: ErrorClass).asJson.noSpaces
    json shouldBe "\"Transient\""
  }

  it should "encode Systemic as string" in {
    val json = (ErrorClass.Systemic: ErrorClass).asJson.noSpaces
    json shouldBe "\"Systemic\""
  }

  it should "round-trip Transient through JSON" in {
    val original: ErrorClass = ErrorClass.Transient
    val json                 = original.asJson
    val decoded              = json.as[ErrorClass]
    decoded shouldBe Right(original)
  }

  it should "round-trip Systemic through JSON" in {
    val original: ErrorClass = ErrorClass.Systemic
    val json                 = original.asJson
    val decoded              = json.as[ErrorClass]
    decoded shouldBe Right(original)
  }

  it should "fail to decode unknown string" in {
    val result = decode[ErrorClass]("\"Unknown\"")
    result.isLeft shouldBe true
  }

  it should "parse from string case-insensitively" in {
    val _ = ErrorClass.fromString("transient") shouldBe Right(ErrorClass.Transient)
    val _ = ErrorClass.fromString("SYSTEMIC") shouldBe Right(ErrorClass.Systemic)
    ErrorClass.fromString("Transient") shouldBe Right(ErrorClass.Transient)
  }

  it should "return Left for unrecognized string" in {
    val result = ErrorClass.fromString("bogus")
    result.isLeft shouldBe true
  }

  it should "expose its label" in {
    val _ = ErrorClass.Transient.label shouldBe "Transient"
    ErrorClass.Systemic.label shouldBe "Systemic"
  }

  "UnrecognizedErrorClass" should "name the rejected value and the valid ones" in {
    val err = UnrecognizedErrorClass("bogus")
    err.getMessage shouldBe "Unrecognized ErrorClass: 'bogus'. Valid values: Transient, Systemic"
  }

  // ── DefaultErrorClassifier ──

  "DefaultErrorClassifier" should "classify all errors as Systemic" in {
    DefaultErrorClassifier.classify(new RuntimeException("test")) shouldBe ErrorClass.Systemic
  }

  it should "classify IllegalArgumentException as Systemic" in {
    DefaultErrorClassifier.classify(new IllegalArgumentException("bad arg")) shouldBe ErrorClass.Systemic
  }

  // ── HttpErrorClassifier ──

  "HttpErrorClassifier" should "classify 429 as Transient" in {
    val classifier = new HttpErrorClassifier(_ => Some(429))
    classifier.classify(new RuntimeException("rate limited")) shouldBe ErrorClass.Transient
  }

  it should "classify 500 as Transient" in {
    val classifier = new HttpErrorClassifier(_ => Some(500))
    classifier.classify(new RuntimeException("internal server error")) shouldBe ErrorClass.Transient
  }

  it should "classify 502 as Transient" in {
    val classifier = new HttpErrorClassifier(_ => Some(502))
    classifier.classify(new RuntimeException("bad gateway")) shouldBe ErrorClass.Transient
  }

  it should "classify 503 as Transient" in {
    val classifier = new HttpErrorClassifier(_ => Some(503))
    classifier.classify(new RuntimeException("service unavailable")) shouldBe ErrorClass.Transient
  }

  it should "classify 504 as Transient" in {
    val classifier = new HttpErrorClassifier(_ => Some(504))
    classifier.classify(new RuntimeException("gateway timeout")) shouldBe ErrorClass.Transient
  }

  it should "classify 400 as Systemic" in {
    val classifier = new HttpErrorClassifier(_ => Some(400))
    classifier.classify(new RuntimeException("bad request")) shouldBe ErrorClass.Systemic
  }

  it should "classify 401 as Systemic" in {
    val classifier = new HttpErrorClassifier(_ => Some(401))
    classifier.classify(new RuntimeException("unauthorized")) shouldBe ErrorClass.Systemic
  }

  it should "classify 403 as Systemic" in {
    val classifier = new HttpErrorClassifier(_ => Some(403))
    classifier.classify(new RuntimeException("forbidden")) shouldBe ErrorClass.Systemic
  }

  it should "classify 404 as Systemic" in {
    val classifier = new HttpErrorClassifier(_ => Some(404))
    classifier.classify(new RuntimeException("not found")) shouldBe ErrorClass.Systemic
  }

  it should "classify unknown status code as Systemic" in {
    val classifier = new HttpErrorClassifier(_ => Some(418))
    classifier.classify(new RuntimeException("i'm a teapot")) shouldBe ErrorClass.Systemic
  }

  it should "fall back to DefaultErrorClassifier when no status extracted" in {
    val classifier = new HttpErrorClassifier(_ => None)
    classifier.classify(new RuntimeException("no status")) shouldBe ErrorClass.Systemic
  }

  // ── RetryConfig PureConfig ──

  "RetryConfig" should "have correct defaults" in {
    val config = RetryConfig()
    val _      = config.maxRetries shouldBe 3
    val _      = config.initialBackoffMs shouldBe 10L
    val _      = config.maxBackoffMs shouldBe 60000L
    config.backoffMultiplier shouldBe 2.0
  }

  it should "round-trip through PureConfig" in {
    val source = ConfigSource.string(
      """|max-retries = 5
         |initial-backoff-ms = 100
         |max-backoff-ms = 30000
         |backoff-multiplier = 3.0
         |""".stripMargin
    )
    val result = source.load[RetryConfig]
    result shouldBe Right(RetryConfig(5, 100L, 30000L, 3.0))
  }

  it should "load defaults from PureConfig when fields are provided" in {
    val source = ConfigSource.string(
      """|max-retries = 3
         |initial-backoff-ms = 10
         |max-backoff-ms = 60000
         |backoff-multiplier = 2.0
         |""".stripMargin
    )
    val result = source.load[RetryConfig]
    result shouldBe Right(RetryConfig())
  }

}
