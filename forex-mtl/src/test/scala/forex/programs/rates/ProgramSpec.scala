package forex.programs.rates

import cats.effect.IO
import cats.syntax.either._
import forex.domain.{ Currency, Price, Rate, Timestamp }
import forex.programs.rates.Protocol.GetRatesRequest
import forex.programs.rates.errors.ProgramError
import forex.services.cache.{ Interpreters => CacheInterpreters }
import forex.services.errors.ServiceError.EmptyResult
import forex.services.rates.{ Interpreters => RateInterpreters }
import forex.services.{ cache, errors, rates }
import io.circe.syntax._
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers
class ProgramSpec extends AsyncFlatSpec with Matchers {

  "ProgramSpec" should "get a rate" in {
    implicit val rateService: rates.Algebra[IO]  = RateInterpreters.dummy[IO]
    implicit val cacheService: cache.Algebra[IO] = CacheInterpreters.memoryCache[IO]
    val pair                                     = Rate.Pair(Currency.USD, Currency.JPY)

    (for {
      either1 <- Program[IO].get(GetRatesRequest(pair.from, pair.to))
      either2 <- cacheService.get(s"${pair.from}${pair.to}")
    } yield {
      (either1, either2) match {
        case (Right(_), Right(Some(_))) => assert(true)
        case _                          => assert(false)
      }
    }).unsafeToFuture()
  }

  it should "get a rate from cache" in {
    implicit val rateService: rates.Algebra[IO]  = RateInterpreters.dummy[IO]
    implicit val cacheService: cache.Algebra[IO] = CacheInterpreters.memoryCache[IO]
    val pair                                     = Rate.Pair(Currency.USD, Currency.JPY)
    val rate                                     = Rate(pair = pair, price = Price(BigDecimal(1)), timestamp = Timestamp.now)

    (for {
      _ <- cacheService.put(s"${pair.from}${pair.to}", rate.asJson.noSpaces)
      either1 <- Program[IO].get(GetRatesRequest(pair.from, pair.to))
      either2 <- cacheService.get(s"${pair.from}${pair.to}")
    } yield {
      (either1, either2) match {
        case (Right(r), Right(Some(json))) => {
          r shouldBe rate
          json shouldBe rate.asJson.noSpaces
        }
        case _ => assert(false)
      }
    }).unsafeToFuture()
  }

  it should "get EmptyResult" in {
    implicit val rateService: rates.Algebra[IO] = new rates.Algebra[IO] {
      override def get(pair: Rate.Pair): IO[Either[errors.ServiceError, Rate]] = IO.pure(EmptyResult.asLeft)
    }
    implicit val cacheService: cache.Algebra[IO] = CacheInterpreters.memoryCache[IO]
    val pair                                     = Rate.Pair(Currency.USD, Currency.JPY)

    (for {
      either1 <- Program[IO].get(GetRatesRequest(pair.from, pair.to))
      either2 <- cacheService.get(s"${pair.from}${pair.to}")
    } yield {
      (either1, either2) match {
        case (Left(ProgramError.EmptyResult), Right(None)) => {
          assert(true)
        }
        case _ => assert(false)
      }
    }).unsafeToFuture()
  }
}
