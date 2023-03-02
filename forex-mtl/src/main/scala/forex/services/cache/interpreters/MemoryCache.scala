package forex.services.cache.interpreters

import cats.Applicative
import cats.syntax.applicative._
import forex.services.cache.Algebra
import forex.services.errors

import scala.collection.mutable

final case class MemoryCache[F[_]: Applicative](cache: mutable.Map[String, String]) extends Algebra[F] {
  override def get(key: String): F[Either[errors.ServiceError, Option[String]]] = cache.get(key).toEither.pure[F]

  override def put(key: String, value: String): F[Either[errors.ServiceError, Unit]] =
    cache.put(key, value).fold(())(_ => ()).toEither.pure[F]

  override def delete(key: String): F[Either[errors.ServiceError, Unit]] =
    cache.remove(key).fold(())(_ => ()).toEither.pure[F]
}
