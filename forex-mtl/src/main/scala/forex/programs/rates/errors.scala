package forex.programs.rates

import forex.services.errors.ServiceError

object errors {

  sealed trait ProgramError extends Exception
  object ProgramError {
    final case class RateLookupFailed(msg: String) extends ProgramError
  }

  def toProgramError(error: ServiceError): ProgramError = error match {
    case ServiceError.OneFrameLookupFailed(msg) => ProgramError.RateLookupFailed(msg)
    case ServiceError.DecodeJsonFailed(msg)     => ProgramError.RateLookupFailed(msg)
    case ServiceError.CacheFailed(throwable)    => ProgramError.RateLookupFailed(throwable.getMessage)
    case ServiceError.WrongUrl(msg)             => ProgramError.RateLookupFailed(msg)
  }
}
