package forex.domain

import io.circe._
import io.circe.generic.semiauto._

final case class Rate(
    pair: Rate.Pair,
    price: Price,
    timestamp: Timestamp
)

object Rate {
  final case class Pair(
      from: Currency,
      to: Currency
  )

  object Pair {
    implicit val encoder: Encoder[Pair] =
      deriveEncoder[Pair]

    implicit val decoder: Decoder[Pair] =
      deriveDecoder[Pair]
  }

  implicit val encoder: Encoder[Rate] =
    deriveEncoder[Rate]

  implicit val decoder: Decoder[Rate] = deriveDecoder[Rate]
}
