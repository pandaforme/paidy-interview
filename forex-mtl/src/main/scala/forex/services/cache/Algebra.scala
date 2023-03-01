package forex.services.cache

import forex.services.errors._

trait Algebra[F[_]] {
  def get(key: String): F[Either[ServiceError, Option[String]]]
  def put(key: String, value: String): F[Either[ServiceError, Unit]]
  def delete(key: String): F[Either[ServiceError, Unit]]
}
