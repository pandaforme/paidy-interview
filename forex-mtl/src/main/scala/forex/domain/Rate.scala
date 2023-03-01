package forex.domain

import io.circe._
import io.circe.generic.semiauto._

case class Rate(
    pair: Pair,
    price: Price,
    timestamp: Timestamp
)

object Rate {
  implicit val encoder: Encoder[Rate] =
    deriveEncoder[Rate]

  implicit val decoder: Decoder[Rate] = deriveDecoder[Rate]
}
