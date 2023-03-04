package forex.services.cache

import cats.Id
import cats.data.EitherT
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class MemoryCacheSpec extends AnyFlatSpec with Matchers {

  "MemoryCache" should "get a value" in {
    val test = """{"name":"test"}"""

    val cache = Interpreters.memoryCache[Id]

    val result = for {
      _ <- EitherT(cache.put("key", test))
      r <- EitherT(cache.get("key"))
      _ <- EitherT(cache.delete("key"))
    } yield { r }

    result.value.fold(_ => assert(false), fb => {
      fb shouldBe Some(test)
    })
  }

  it should "not get a value" in {
    val cache = Interpreters.memoryCache[Id]

    val result = for {
      r <- EitherT(cache.get("key"))
    } yield { r }

    result.value.fold(_ => assert(false), fb => {
      fb shouldBe None
    })
  }
}
