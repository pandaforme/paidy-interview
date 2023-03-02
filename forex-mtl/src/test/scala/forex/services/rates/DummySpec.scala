package forex.services.rates

import cats.Id
import cats.data.EitherT
import forex.domain.{Currency, Rate, Timestamp}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class DummySpec extends AnyFlatSpec with Matchers {

  "Dummy" should "get a dummy rate" in {
    val pair = Rate.Pair(Currency.USD, Currency.JPY)

    val value = for {
      r <- EitherT(
            Interpreters.dummy[Id].get(pair)
          )
    } yield { r }

    value.value.fold(_ => assert(false), fb => {
      fb.pair shouldBe pair
      fb.price.value shouldBe BigDecimal(100)
      fb.timestamp.value should be < Timestamp.now.value
    })
  }
}
