package forex.services.oneforge

import cats.data.EitherT
import cats.effect.concurrent.Ref
import cats.effect.{ ContextShift, IO, Resource, Timer }
import eu.timepit.refined.auto._
import forex.config.OneFrameConfig
import forex.domain.{ Currency, OneFrame, Rate }
import forex.services.errors.ServiceError
import forex.services.errors.ServiceError.{ DecodeJsonFailed, OneFrameLookupFailed }
import forex.services.rates.Interpreters
import org.http4s.circe.jsonOf
import org.http4s.client.Client
import org.http4s.headers.`Content-Type`
import org.http4s.{ EntityDecoder, MediaType, Response, Status }
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.concurrent.ExecutionContext.global
import scala.concurrent.duration._
import scala.language.postfixOps

class OneFrameSpec extends AsyncFlatSpec with Matchers {
  private implicit val cs: ContextShift[IO]                       = IO.contextShift(global)
  private implicit val timer: Timer[IO]                           = IO.timer(global)
  private implicit val decoder: EntityDecoder[IO, List[OneFrame]] = jsonOf[IO, List[OneFrame]]

  "One frame" should "convert from one currency to another successfully" in {
    val config = OneFrameConfig("api key", "http://test.com", 1 minutes, 1)

    val resourceClient: Resource[IO, Client[IO]] = httpClient(
      """[{"from":"USD","to":"JPY","bid":0.6118225421857174,"ask":0.8243869101616611,"price":0.71810472617368925,"time_stamp":"2023-03-02T06:22:53.478Z"}]""",
      Status.Ok
    )
    val oneFrame = Interpreters.live[IO](config, resourceClient)
    val result = for {
      r <- EitherT(oneFrame.get(Rate.Pair(Currency.USD, Currency.EUR)))
    } yield { r }

    result.value
      .map(
        _.fold(
          _ => assert(false),
          _ => assert(true)
        )
      )
      .unsafeToFuture()
  }

  it should "get an error when using a illegal api key" in {
    val config = OneFrameConfig("api key", "http://test.com", 1 minutes, 1)

    val resourceClient: Resource[IO, Client[IO]] = httpClient(
      """{"error":"Forbidden"}""",
      Status.Ok
    )
    val oneFrame = Interpreters.live[IO](config, resourceClient)
    val result = for {
      r <- EitherT(oneFrame.get(Rate.Pair(Currency.USD, Currency.JPY)))
    } yield {
      r
    }

    result.value
      .map(
        _.fold(
          fa => {
            fa shouldBe a[ServiceError]
            fa.asInstanceOf[DecodeJsonFailed].msg should not be ""
          },
          _ => assert(false)
        )
      )
      .unsafeToFuture()
  }

  it should "retry when oneframe service is timeout" in {
    val config = OneFrameConfig("api key", "http://test.com", 1 minutes, 3)

    val result: IO[(Either[ServiceError, Rate], Int)] = for {
      ref <- Ref.of[IO, Int](0)
      resourceClient = httpClient(
        """{"error":"Forbidden"}""",
        Status.RequestTimeout,
        ref
      )
      oneFrame = Interpreters.live[IO](config, resourceClient)
      either <- oneFrame.get(Rate.Pair(Currency.USD, Currency.JPY))
      count <- ref.get
    } yield {
      (either, count)
    }

    result
      .map {
        case (Left(OneFrameLookupFailed(_)), 4) => assert(true)
        case _                                  => assert(false)
      }
      .unsafeToFuture()
  }

  private def httpClient(body: String, status: Status): Resource[IO, Client[IO]] = {
    val client: Client[IO] = Client.apply[IO] { _ =>
      val response: Response[IO] =
        Response[IO](status).withEntity(body).putHeaders(`Content-Type`(MediaType.application.json))
      Resource.eval(IO.pure(response))
    }

    Resource.pure[IO, Client[IO]](client)
  }
  private def httpClient(body: String, status: Status, ref: Ref[IO, Int]): Resource[IO, Client[IO]] = {
    val client: Client[IO] = Client.apply[IO] { _ =>
      val response: IO[Response[IO]] = for {
        _ <- ref.update(_ + 1)
      } yield {
        Response[IO](status).withEntity(body).putHeaders(`Content-Type`(MediaType.application.json))
      }

      Resource.eval(response)
    }

    Resource.pure[IO, Client[IO]](client)
  }
}
