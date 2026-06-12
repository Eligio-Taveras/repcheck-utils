package com.repcheck.utils.errors

import java.net.ConnectException
import java.util.concurrent.TimeoutException

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class NetworkAwareHttpClassifierSpec extends AnyFlatSpec with Matchers {

  final private case class WireError(status: Int) extends Exception(s"wire error $status")

  private val classifier = new NetworkAwareHttpClassifier({
    case WireError(status) => Some(status)
    case _                 => None
  })

  "the classifier" should "treat timeouts and refused connections as transient" in {
    classifier.classify(new TimeoutException("slow")) shouldBe ErrorClass.Transient
    classifier.classify(new ConnectException("refused")) shouldBe ErrorClass.Transient
  }

  it should "treat extracted 429 and 5xx statuses as transient" in {
    List(429, 500, 502, 503, 504).foreach { status =>
      classifier.classify(WireError(status)) shouldBe ErrorClass.Transient
    }
  }

  it should "treat other extracted statuses as systemic" in {
    List(400, 401, 404, 422).foreach(status => classifier.classify(WireError(status)) shouldBe ErrorClass.Systemic)
  }

  it should "treat unextractable errors as systemic" in {
    classifier.classify(new RuntimeException("boom")) shouldBe ErrorClass.Systemic
  }

}
