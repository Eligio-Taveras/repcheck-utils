package com.repcheck.utils.tags

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class E2ETestSpec extends AnyFlatSpec with Matchers {

  "E2ETest" should "keep the canonical tag name existing run commands filter on" in {
    E2ETest.name shouldBe "com.repcheck.tags.E2ETest"
  }

}
