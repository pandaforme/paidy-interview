package forex.programs.rates

import cats.Monad
import cats.data.EitherT
import cats.implicits._
import forex.domain._
import forex.programs.rates.errors._
import forex.services.errors.ServiceError
import forex.services.errors.ServiceError.DecodeJsonFailed
import forex.services.{ CacheService, RatesService }
import io.circe.parser.decode
import io.circe.syntax._

class Program[F[_]](
    implicit
    M: Monad[F],
    RatesService: RatesService[F],
    CacheService: CacheService[F]
) extends Algebra[F] {

  override def get(request: Protocol.GetRatesRequest): F[ProgramError Either Rate] =
    (for {
      key <- EitherT.pure[F, ProgramError](s"${request.from}${request.to}")
      cacheResult <- EitherT(CacheService.get(key)).leftMap(toProgramError)
      result <- EitherT {
                 cacheResult match {
                   case None =>
                     getAndSet(request)
                   case Some(s) => Monad[F].pure(toRate(s))
                 }
               }.leftMap(toProgramError)
    } yield { result }).value

  private def getAndSet(
      request: Protocol.GetRatesRequest
  )(implicit M: Monad[F], RatesService: RatesService[F], CacheService: CacheService[F]): F[Either[ServiceError, Rate]] =
    (for {
      rate <- EitherT(RatesService.get(Pair(request.from, request.to)))
      _ <- EitherT(CacheService.put(s"${request.from}${request.to}", rate.asJson.noSpaces))
    } yield {
      rate
    }).value

  private def toRate(s: String): Either[ServiceError, Rate] =
    decode[Rate](s).leftMap[ServiceError](_ => DecodeJsonFailed(s"Failed to decode $s into Rate"))
}

object Program {
  def apply[F[_]: Monad: RatesService: CacheService]: Algebra[F] = new Program[F]
}
