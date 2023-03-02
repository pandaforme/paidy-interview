package forex.services.rates.interpreters

import cats.data.EitherT
import cats.effect.{ Concurrent, Resource, Timer }
import cats.syntax.either._
import cats.syntax.functor._
import eu.timepit.refined.auto._
import forex.config.OneFrameConfig
import forex.domain.{ OneFrame, Rate }
import forex.services.errors.ServiceError.{ DecodeJsonFailed, EmptyResult, OneFrameLookupFailed, WrongUrl }
import forex.services.errors._
import forex.services.rates.Algebra
import org.http4s._
import org.http4s.client.Client
import org.http4s.client.middleware.{ Retry, RetryPolicy }
import org.http4s.headers.Accept

final case class OneFrameLive[F[_]: Concurrent: Timer](
    oneFrameConfig: OneFrameConfig,
    resourceClient: Resource[F, Client[F]]
)(implicit oneFrameEntityDecoder: EntityDecoder[F, List[OneFrame]])
    extends Algebra[F] {

  override def get(pair: Rate.Pair): F[Either[ServiceError, Rate]] = resourceClient.use { client =>
    (for {
      uri <- EitherT.fromEither[F](
              Uri
                .fromString(s"${oneFrameConfig.host}/rates?pair=${pair.from}${pair.to}")
                .leftMap(pf => WrongUrl(pf.details))
            )
      retryPolicy <- EitherT.pure[F, ServiceError](
                      RetryPolicy[F](
                        backoff = RetryPolicy.exponentialBackoff(oneFrameConfig.maxWait, oneFrameConfig.maxRetry)
                      )
                    )
      request <- EitherT.pure[F, ServiceError](
                  Request[F](
                    method = Method.GET,
                    uri = uri,
                    headers = Headers.of(
                      Header(name = "token", oneFrameConfig.key),
                      Accept(MediaType.application.json)
                    )
                  )
                )
      clientWithRetries <- EitherT.pure[F, ServiceError](Retry[F](retryPolicy)(client))
      result <- EitherT[F, ServiceError, List[OneFrame]](clientWithRetries.run(request).use {
                 case Status.Successful(r) =>
                   r.attemptAs[List[OneFrame]]
                     .leftMap(df => DecodeJsonFailed(df.message))
                     .value
                     .map(_.left.map[ServiceError](identity))
                 case r =>
                   r.as[String]
                     .map(
                       s => Left(OneFrameLookupFailed(s"Request $uri failed with status ${r.status.code} and body $s"))
                     )
               })
      oneFrame <- EitherT.fromEither[F](toEither(result))
    } yield {
      Rate(Rate.Pair(oneFrame.from, oneFrame.to), oneFrame.price, oneFrame.timestamp)
    }).value
  }

  private def toEither(list: List[OneFrame]): Either[ServiceError, OneFrame] =
    list match {
      case Nil    => EmptyResult.asLeft
      case h :: _ => h.asRight
    }
}
