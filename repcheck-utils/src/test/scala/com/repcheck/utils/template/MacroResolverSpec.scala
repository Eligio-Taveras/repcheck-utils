package com.repcheck.utils.template

import java.time.Instant

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class MacroResolverSpec extends AnyFlatSpec with Matchers {

  private val context = MacroContext(
    workflowRunId = 42L,
    timestamp = Instant.parse("2026-06-10T12:00:00Z"),
    date = "2026-06-10",
    messagePayload = Map("billId" -> "hr-1234", "dollar" -> "$pecial"),
  )

  "MacroResolver" should "resolve workflow_run_id, date, and timestamp" in {
    MacroResolver.resolve("run={{workflow_run_id}} on {{date}} at {{timestamp}}", context) shouldBe
      "run=42 on 2026-06-10 at 2026-06-10T12:00:00Z"
  }

  it should "resolve message.* fields from the payload" in {
    MacroResolver.resolve("bill={{message.billId}}", context) shouldBe "bill=hr-1234"
  }

  it should "leave a missing message field unresolved" in {
    MacroResolver.resolve("{{message.nope}}", context) shouldBe "{{message.nope}}"
  }

  it should "leave an unknown macro unresolved" in {
    MacroResolver.resolve("{{mystery}}", context) shouldBe "{{mystery}}"
  }

  it should "leave a template with no macros untouched" in {
    MacroResolver.resolve("plain text", context) shouldBe "plain text"
  }

  it should "quote regex-special replacement characters safely" in {
    MacroResolver.resolve("{{message.dollar}}", context) shouldBe "$pecial"
  }

}
