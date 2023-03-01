package forex.services

object errors {

  sealed trait ServiceError
  object ServiceError {
    final case class OneFrameLookupFailed(msg: String) extends ServiceError
    final case class DecodeJsonFailed(msg: String) extends ServiceError
    final case class CacheFailed(throwable: Throwable) extends ServiceError
    final case class WrongUrl(msg: String) extends ServiceError
  }

}
