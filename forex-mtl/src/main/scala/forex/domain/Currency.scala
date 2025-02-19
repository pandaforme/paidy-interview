package forex.domain

import cats.Show
import cats.syntax.either._
import io.circe._

sealed trait Currency

object Currency {
  case object AUD extends Currency
  case object CAD extends Currency
  case object CHF extends Currency
  case object EUR extends Currency
  case object GBP extends Currency
  case object NZD extends Currency
  case object JPY extends Currency
  case object SGD extends Currency
  case object USD extends Currency
  case object UNSUPPORTED_CURRENCY extends Currency

  implicit val show: Show[Currency] = Show.show {
    case AUD                  => "AUD"
    case CAD                  => "CAD"
    case CHF                  => "CHF"
    case EUR                  => "EUR"
    case GBP                  => "GBP"
    case NZD                  => "NZD"
    case JPY                  => "JPY"
    case SGD                  => "SGD"
    case USD                  => "USD"
    case UNSUPPORTED_CURRENCY => "UNSUPPORTED_CURRENCY"
  }

  def fromString(s: String): Currency = s.toUpperCase match {
    case "AUD" => AUD
    case "CAD" => CAD
    case "CHF" => CHF
    case "EUR" => EUR
    case "GBP" => GBP
    case "NZD" => NZD
    case "JPY" => JPY
    case "SGD" => SGD
    case "USD" => USD
    case _     => UNSUPPORTED_CURRENCY
  }

  implicit val encoder: Encoder[Currency] =
    Encoder.instance[Currency] {
      show.show _ andThen Json.fromString
    }

  implicit val decoder: Decoder[Currency] =
    Decoder.instance[Currency] { c =>
      c.as[String].fold(fa => fa.asLeft, fb => fromString(fb).asRight)
    }
}
