package forex.services.cache

import cats.data.EitherT
import cats.effect.{ ContextShift, IO, Timer }
import cats.syntax.either._
import eu.timepit.refined.auto._
import forex.config.CacheConfig
import forex.services.errors.ServiceError
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers

import java.util.concurrent.TimeUnit
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.Random

class CaffeineCacheSpec extends AsyncFlatSpec with Matchers {
  import scala.concurrent.ExecutionContext.global
  implicit val cs: ContextShift[IO] = IO.contextShift(global)
  implicit val timer: Timer[IO]     = IO.timer(global)

  "CaffeineCache" should "get a value and value never expires" in {
    val test = s"""{"name":"test${Random.nextInt()}"}"""

    val cache =
      Interpreters.caffeineCache[IO](CacheConfig(1000L, Long.MaxValue nanoseconds))

    val result = for {
      _ <- EitherT(cache.put("key", test))
      r <- EitherT(cache.get("key"))
      _ <- EitherT(cache.delete("key"))
    } yield { r }

    result.value
      .map(_.fold(_ => assert(false), fb => {
        fb shouldBe Some(test)
      }))
      .unsafeToFuture()
  }

  it should "not get a value and value never expires" in {
    val cache =
      Interpreters.caffeineCache[IO](CacheConfig(1000L, Long.MaxValue nanoseconds))

    val result = for {
      r <- EitherT(cache.get("key"))
    } yield { r }

    result.value
      .map(_.fold(_ => assert(false), fb => {
        fb shouldBe None
      }))
      .unsafeToFuture()
  }

  it should "get None when the value has expired" in {
    val test                      = s"""{"name":"test${Random.nextInt()}"}"""
    implicit val timer: Timer[IO] = IO.timer(global)

    val cache = Interpreters.caffeineCache[IO](CacheConfig(1000L, 1 seconds))
    val result = for {
      _ <- EitherT(cache.put("key", test))
      _ <- EitherT(sleep(2000))
      r <- EitherT(cache.get("key"))
      _ <- EitherT(cache.delete("key"))
    } yield { r }

    result.value
      .map(_.fold(_ => assert(false), fb => {
        fb shouldBe None
      }))
      .unsafeToFuture()
  }

  it should "get a value when the value has not expired" in {
    val test                      = s"""{"name":"test${Random.nextInt()}"}"""
    implicit val timer: Timer[IO] = IO.timer(global)

    val cache = Interpreters.caffeineCache[IO](CacheConfig(1000L, 5 seconds))
    val result = for {
      _ <- EitherT(cache.put("key", test))
      _ <- EitherT(sleep(1000))
      r <- EitherT(cache.get("key"))
      _ <- EitherT(cache.delete("key"))
    } yield { r }

    result.value
      .map(_.fold(_ => assert(false), fb => {
        fb shouldBe Some(test)
      }))
      .unsafeToFuture()
  }

  private def sleep(sleepTimeInMilliSecond: Long)(implicit timer: Timer[IO]): IO[Either[ServiceError, Unit]] =
    IO.sleep(FiniteDuration.apply(sleepTimeInMilliSecond, TimeUnit.MILLISECONDS)).map(_ => ().asRight)
}
