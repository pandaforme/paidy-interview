package forex.services.rates.interpreters

import cats.data.EitherT
import cats.effect.{ ContextShift, IO, Timer }
import cats.syntax.either._
import eu.timepit.refined.auto._
import forex.config.oneFrameConfig
import forex.domain.{ Pair, Rate }
import forex.services.errors.ServiceError.{ DecodeJsonFailed, OneFrameLookupFailed, WrongUrl }
import forex.services.errors._
import forex.services.rates.Algebra
import org.http4s._
import org.http4s.circe.jsonOf
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.client.middleware.{ Retry, RetryPolicy }
import org.http4s.headers.Accept

import scala.concurrent.ExecutionContext.Implicits.global

final case class OneFrameLive(oneFrameConfig: oneFrameConfig) extends Algebra[IO] {

  implicit val cs: ContextShift[IO]                 = IO.contextShift(global)
  implicit val timer: Timer[IO]                     = IO.timer(global)
  implicit val rateDecoder: EntityDecoder[IO, Rate] = jsonOf[IO, Rate]

  override def get(pair: Pair): IO[Either[ServiceError, Rate]] = BlazeClientBuilder[IO](global).resource.use { client =>
    (for {
      uri <- EitherT.fromEither[IO](
              Uri
                .fromString(s"${oneFrameConfig.host}/rates?pair=${pair.from}${pair.to}")
                .leftMap(pf => WrongUrl(pf.details))
            )
      retryPolicy <- EitherT.pure[IO, ServiceError](
                      RetryPolicy[IO](
                        backoff = RetryPolicy.exponentialBackoff(oneFrameConfig.maxWait, oneFrameConfig.maxRetry)
                      )
                    )
      request <- EitherT.pure[IO, ServiceError](
                  Request[IO](
                    method = Method.GET,
                    uri = uri,
                    headers = Headers.of(
                      Header(name = "token", oneFrameConfig.key),
                      Accept(MediaType.application.json)
                    )
                  )
                )
      clientWithRetries <- EitherT.pure[IO, ServiceError](Retry[IO](retryPolicy)(client))
      result <- EitherT[IO, ServiceError, Rate](clientWithRetries.run(request).use {
                 case Status.Successful(r) =>
                   r.attemptAs[Rate].leftMap(df => DecodeJsonFailed(df.message)).value
                 case r =>
                   r.as[String]
                     .map(
                       s => Left(OneFrameLookupFailed(s"Request $uri failed with status ${r.status.code} and body $s"))
                     )
               })
    } yield {
      result
    }).value
  }

}
