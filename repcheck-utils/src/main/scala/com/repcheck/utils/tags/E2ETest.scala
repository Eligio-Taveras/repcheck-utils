package com.repcheck.utils.tags

import org.scalatest.Tag

/**
 * Tag for tests that need live data or real infrastructure. Excluded from `sbt test`; run explicitly via `sbt "testOnly
 * -- -n com.repcheck.tags.E2ETest"`. The tag NAME stays `com.repcheck.tags.E2ETest` so existing run commands keep
 * working as repos migrate from their local tag objects.
 */
object E2ETest extends Tag("com.repcheck.tags.E2ETest")
