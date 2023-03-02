package forex.services.rates.interpreters

import cats.Applicative
import cats.syntax.applicative._
import cats.syntax.either._
import forex.domain.{ Price, Rate, Timestamp }
import forex.services.errors._
import forex.services.rates.Algebra

class OneFrameDummy[F[_]: Applicative] extends Algebra[F] {

  override def get(pair: Rate.Pair): F[ServiceError Either Rate] =
    Rate(pair, Price(BigDecimal(100)), Timestamp.now).asRight[ServiceError].pure[F]

}
