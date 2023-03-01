package forex.services.cache

import forex.services.errors.ServiceError
import forex.services.errors.ServiceError.CacheFailed

package object interpreters {
  implicit class EitherUtils[A](a: => A) {
    def toEither: Either[ServiceError, A] =
      try {
        Right(a)
      } catch {
        case e: Throwable => Left(CacheFailed(e))
      }
  }
}
