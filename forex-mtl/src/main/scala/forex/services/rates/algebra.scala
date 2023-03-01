package forex.services.rates

import forex.domain.{ Pair, Rate }
import forex.services.errors._

trait Algebra[F[_]] {
  def get(pair: Pair): F[ServiceError Either Rate]
}
