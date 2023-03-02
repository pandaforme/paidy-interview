package forex.services.cache

import cats.syntax.either._
import forex.services.errors.ServiceError
import forex.services.errors.ServiceError.CacheFailed

package object interpreters {
  implicit class EitherUtils[A](a: => A) {
    def toEither: Either[ServiceError, A] =
      try {
        a.asRight
      } catch {
        case e: Throwable => Left(CacheFailed(e))
      }
  }
}
