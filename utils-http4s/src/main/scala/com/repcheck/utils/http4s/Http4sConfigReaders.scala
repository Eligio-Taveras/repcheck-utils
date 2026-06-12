package com.repcheck.utils.http4s

import org.http4s.Uri

import pureconfig.ConfigReader
import pureconfig.error.CannotConvert

/**
 * PureConfig bridges for http4s types, split from the core so consumers without an HTTP layer stay http4s-free. Import
 * where a config case class derives a reader over a `Uri` field: `import
 * com.repcheck.utils.http4s.Http4sConfigReaders.given`.
 */
object Http4sConfigReaders {

  given uriConfigReader: ConfigReader[Uri] =
    ConfigReader[String].emap(raw => Uri.fromString(raw).left.map(e => CannotConvert(raw, "Uri", e.message)))

}
