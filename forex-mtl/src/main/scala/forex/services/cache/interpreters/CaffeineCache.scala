package forex.services.cache.interpreters

import cats.effect.IO
import com.github.blemale.scaffeine
import forex.services.cache.Algebra
import forex.services.errors._

final case class CaffeineCache(cache: scaffeine.Cache[String, String]) extends Algebra[IO] {
  override def get(key: String): IO[Either[ServiceError, Option[String]]] = IO(cache.getIfPresent(key).toEither)

  override def put(key: String, value: String): IO[Either[ServiceError, Unit]] = IO(cache.put(key, value).toEither)

  override def delete(key: String): IO[Either[ServiceError, Unit]] = IO(cache.invalidate(key).toEither)
}
