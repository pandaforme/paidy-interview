package forex.services.cache.interpreters

import cats.effect.Concurrent
import com.github.blemale.scaffeine
import forex.services.cache.Algebra
import forex.services.errors._

final case class CaffeineCache[F[_]: Concurrent](cache: scaffeine.Cache[String, String]) extends Algebra[F] {

  override def get(key: String): F[Either[ServiceError, Option[String]]] =
    Concurrent[F].delay(cache.getIfPresent(key).toEither)

  override def put(key: String, value: String): F[Either[ServiceError, Unit]] =
    Concurrent[F].delay(cache.put(key, value).toEither)

  override def delete(key: String): F[Either[ServiceError, Unit]] = Concurrent[F].delay(cache.invalidate(key).toEither)

}
