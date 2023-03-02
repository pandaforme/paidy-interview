package forex.services.rates

import forex.domain.Rate
import forex.services.errors._

trait Algebra[F[_]] {
  def get(pair: Rate.Pair): F[ServiceError Either Rate]
}
