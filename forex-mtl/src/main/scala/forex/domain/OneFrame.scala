package forex.domain

import io.circe._

final case class OneFrame(
    from: Currency,
    to: Currency,
    bid: Price,
    ask: Price,
    price: Price,
    timestamp: Timestamp
)

object OneFrame {
  implicit val encodeUser: Encoder[OneFrame] =
    Encoder.forProduct6("from", "to", "bid", "ask", "price", "time_stamp")(
      of => (of.from, of.to, of.bid, of.ask, of.price, of.timestamp)
    )

  implicit val encodeBar: Decoder[OneFrame] =
    Decoder.forProduct6("from", "to", "bid", "ask", "price", "time_stamp")(OneFrame.apply)
}
