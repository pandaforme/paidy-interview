package forex.domain

import io.circe.{ Decoder, Encoder }
import io.circe.generic.extras.semiauto.{ deriveUnwrappedDecoder, deriveUnwrappedEncoder }

import java.time.OffsetDateTime

case class Timestamp(value: OffsetDateTime) extends AnyVal

object Timestamp {
  def now: Timestamp =
    Timestamp(OffsetDateTime.now)

  implicit val encoder: Encoder[Timestamp] =
    deriveUnwrappedEncoder[Timestamp]

  implicit val decoder: Decoder[Timestamp] = deriveUnwrappedDecoder[Timestamp]
}
