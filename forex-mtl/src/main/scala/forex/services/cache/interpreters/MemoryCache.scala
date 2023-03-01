package forex.services.cache.interpreters

import cats.Id
import forex.services.cache.Algebra
import forex.services.errors

import scala.collection.mutable

final case class MemoryCache() extends Algebra[Id] {
  private val cache: mutable.Map[String, String] = scala.collection.mutable.Map.empty

  override def get(key: String): Id[Either[errors.ServiceError, Option[String]]] = cache.get(key).toEither

  override def put(key: String, value: String): Id[Either[errors.ServiceError, Unit]] =
    cache.put(key, value).fold(())(_ => ()).toEither

  override def delete(key: String): Id[Either[errors.ServiceError, Unit]] = cache.remove(key).fold(())(_ => ()).toEither
}
