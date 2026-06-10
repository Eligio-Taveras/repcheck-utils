package com.repcheck.utils.codecs

import java.time.format.DateTimeFormatter
import java.time.{LocalDate, ZonedDateTime}
import java.util.UUID

import io.circe.{Decoder, Encoder}

object DateTimeCodecs {

  implicit val zonedDateTimeDecoder: Decoder[ZonedDateTime] =
    Decoder.decodeString.emap { str =>
      scala.util
        .Try {
          ZonedDateTime.parse(str, DateTimeFormatter.ISO_ZONED_DATE_TIME)
        }
        .toEither
        .left
        .map(error => s"Failed to decode datetime: ${error.getMessage}")
    }

  implicit val zonedDateTimeEncoder: Encoder[ZonedDateTime] =
    Encoder.encodeString.contramap[ZonedDateTime](dt => dt.format(DateTimeFormatter.ISO_ZONED_DATE_TIME))

  implicit val localDateDecoder: Decoder[LocalDate] =
    Decoder.decodeString.emap { str =>
      scala.util
        .Try {
          LocalDate.parse(str, DateTimeFormatter.ISO_LOCAL_DATE)
        }
        .toEither
        .left
        .map(error => s"Failed to decode date: ${error.getMessage}")
    }

  implicit val localDateEncoder: Encoder[LocalDate] =
    Encoder.encodeString.contramap[LocalDate](d => d.format(DateTimeFormatter.ISO_LOCAL_DATE))

  implicit val uuidDecoder: Decoder[UUID] =
    Decoder.decodeString.emap { str =>
      scala.util
        .Try {
          UUID.fromString(str)
        }
        .toEither
        .left
        .map(error => s"Failed to decode UUID: ${error.getMessage}")
    }

  implicit val uuidEncoder: Encoder[UUID] =
    Encoder.encodeString.contramap[UUID](_.toString)

}
