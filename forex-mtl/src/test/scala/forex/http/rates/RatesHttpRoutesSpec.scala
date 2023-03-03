package forex.http.rates

import cats.effect.IO
import cats.syntax.either._
import forex.domain.{ Currency, Price, Rate, Timestamp }
import forex.http.rates.Protocol.GetApiResponse
import forex.programs.RatesProgram
import forex.programs.rates.Protocol.GetRatesRequest
import forex.programs.rates.errors.ProgramError
import forex.programs.rates.errors.ProgramError.{ EmptyResult, RateLookupFailed }
import org.http4s.circe.jsonOf
import org.http4s.implicits.http4sLiteralsSyntax
import org.http4s.{ EntityDecoder, Method, Request, Status }
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers

class RatesHttpRoutesSpec extends AsyncFlatSpec with Matchers {
  implicit val decoder: EntityDecoder[IO, GetApiResponse] = jsonOf[IO, GetApiResponse]

  "RatesHttpRoutesSpec" should "get a rate" in {
    val pair = Rate.Pair(Currency.USD, Currency.JPY)
    val rate = Rate(pair = pair, price = Price(BigDecimal(1)), timestamp = Timestamp.now)
    val ratesProgram = new RatesProgram[IO] {
      override def get(request: GetRatesRequest): IO[Either[ProgramError, Rate]] = IO.pure(rate.asRight)
    }

    (for {
      response <- new RatesHttpRoutes[IO](ratesProgram)
                   .routes(Request(method = Method.GET, uri = uri"/rates?from=USD&to=JPY"))
                   .value
    } yield {
      response match {
        case Some(s) =>
          s.status shouldBe Status.Ok
          s.as[GetApiResponse].unsafeRunSync() shouldBe GetApiResponse(
            from = pair.from,
            to = pair.to,
            price = rate.price,
            timestamp = rate.timestamp
          )
        case None => assert(false)
      }
    }).unsafeToFuture()
  }

  it should "return NotFound" in {
    val ratesProgram = new RatesProgram[IO] {
      override def get(request: GetRatesRequest): IO[Either[ProgramError, Rate]] = IO.pure(EmptyResult.asLeft)
    }

    (for {
      response <- new RatesHttpRoutes[IO](ratesProgram)
                   .routes(Request(method = Method.GET, uri = uri"/rates?from=USD&to=JPY"))
                   .value
    } yield {
      response match {
        case Some(s) =>
          s.status shouldBe Status.NotFound
        case None => assert(false)
      }
    }).unsafeToFuture()
  }

  it should "return InternalServerError" in {
    val ratesProgram = new RatesProgram[IO] {
      override def get(request: GetRatesRequest): IO[Either[ProgramError, Rate]] =
        IO.pure(RateLookupFailed("BOOM!!!").asLeft)
    }

    (for {
      response <- new RatesHttpRoutes[IO](ratesProgram)
                   .routes(Request(method = Method.GET, uri = uri"/rates?from=USD&to=JPY"))
                   .value
    } yield {
      response match {
        case Some(s) =>
          s.status shouldBe Status.InternalServerError
        case None => assert(false)
      }
    }).unsafeToFuture()
  }
}
