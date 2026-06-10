package com.repcheck.utils.tags

import org.scalatest.Tag

/**
 * Tag for tests that need a real Docker container. Repos that exclude them from `sbt test` filter with `-l
 * DockerRequired` and run them via their `dockerTest` alias; the tag NAME matches the existing per-repo objects so
 * those filters keep working as repos migrate.
 */
object DockerRequired extends Tag("DockerRequired")
