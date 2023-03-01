package forex.domain

import io.circe._
import io.circe.generic.semiauto._

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
