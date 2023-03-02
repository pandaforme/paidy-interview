//package forex.processes.rates
//
//import java.nio.ByteBuffer
//
//import scala.concurrent.duration._
//
//import cats.implicits._
//import com.softwaremill.sttp.Method
//import com.softwaremill.sttp.asynchttpclient.monix.AsyncHttpClientMonixBackend
//import com.softwaremill.sttp.testing.SttpBackendStub
//import eu.timepit.refined.auto._
//import forex.config.{ CacheConfig, ForgeConfig }
//import forex.domain.{ Currency, Price, Rate, Timestamp }
//import forex.main.{ AppEffect, AppStack }
//import forex.processes.ProcessError.ForgeError
//import forex.{ processes ⇒ p, services ⇒ s }
//import io.circe.syntax._
//import monix.eval.Task
//import monix.execution.Scheduler.Implicits.global
//import monix.reactive.Observable
//import org.atnos.eff.syntax.addon.monix.task._
//import org.scalatest.{ AsyncFlatSpec, Matchers }
//
//class ProcessesSpec extends AsyncFlatSpec with Matchers {
//
//  "ProcessesSpec" should "get a rate when use dummy instances" in {
//    implicit lazy val _oneForge: s.OneForge[AppEffect] =
//      s.OneForge.dummy[AppStack]
//
//    implicit lazy val _cache: s.Cache[AppEffect] =
//      s.Cache.mapCache[AppStack]
//
//    implicit lazy val _apply: s.Apply[AppEffect] =
//      s.Apply.monixTask[AppStack]
//
//    val result = p.Rates[AppEffect].get(GetRequest(Currency.USD, Currency.EUR))
//
//    for {
//      _ ← result.runAsync.runAsync.map(
//        _.fold(
//          _ ⇒ assert(false),
//          fb ⇒ {
//            fb.pair shouldBe Rate.Pair(Currency.USD, Currency.EUR)
//            fb.price.value shouldBe BigDecimal(100)
//            fb.timestamp.value should be < Timestamp.now.value
//          }
//        )
//      )
//      _ ← _cache
//        .get(s"${Currency.USD}${Currency.EUR}")
//        .runAsync
//        .runAsync
//        .map(_.fold(_ ⇒ assert(false), fb ⇒ fb shouldBe defined))
//    } yield {
//      assert(true)
//    }
//  }
//
//  it should "get a rate from one forge service when use real instances" in {
//    val forgeConfig =
//      ForgeConfig("api key", "http://test.com", None, None)
//
//    val cacheConfig = CacheConfig(1000, Long.MaxValue nanoseconds)
//
//    implicit val testingBackend =
//      SttpBackendStub[Task, Observable[ByteBuffer], Observable[ByteBuffer]](AsyncHttpClientMonixBackend())
//        .whenRequestMatches(
//          p ⇒
//            p.uri.paramsSeq.equals(
//              List(
//                ("from", Currency.USD.asInstanceOf[Currency].show),
//                ("to", Currency.EUR.asInstanceOf[Currency].show),
//                ("quantity", "1"),
//                ("api_key", "api key")
//              )
//            ) && p.method == Method.GET
//        )
//        .thenRespond("""{"value":0.842162,"text":"1 USD is worth 0.842162 EUR","timestamp":1513840116}""")
//
//    implicit lazy val _oneForge: s.OneForge[AppEffect] =
//      s.OneForge.live[AppStack](forgeConfig)
//
//    implicit lazy val _cache: s.Cache[AppEffect] =
//      s.Cache.caffeineCache[AppStack](cacheConfig)
//
//    implicit lazy val _apply: s.Apply[AppEffect] =
//      s.Apply.monixTask[AppStack]
//
//    val result = p.Rates[AppEffect].get(GetRequest(Currency.USD, Currency.EUR))
//
//    for {
//      _ ← result.runAsync.runAsync.map(
//        _.fold(
//          _ ⇒ assert(false),
//          fb ⇒ {
//            fb.pair shouldBe Rate.Pair(Currency.USD, Currency.EUR)
//            fb.price.value shouldBe a[BigDecimal]
//            fb.timestamp.value should be < Timestamp.now.value
//          }
//        )
//      )
//      _ ← _cache
//        .get(s"${Currency.USD}${Currency.EUR}")
//        .runAsync
//        .runAsync
//        .map(_.fold(_ ⇒ assert(false), fb ⇒ fb shouldBe defined))
//    } yield {
//      assert(true)
//    }
//  }
//
//  it should "get a rate from cache when use real instances" in {
//    val forgeConfig =
//      ForgeConfig("api key", "http://test.com", None, None)
//
//    val cacheConfig = CacheConfig(1000, Long.MaxValue nanoseconds)
//
//    val rate = Rate(Rate.Pair(Currency.USD, Currency.EUR), Price(1234567L), Timestamp.now)
//
//    implicit val testingBackend =
//      SttpBackendStub[Task, Observable[ByteBuffer], Observable[ByteBuffer]](AsyncHttpClientMonixBackend()).whenAnyRequest
//        .thenRespondNotFound()
//
//    implicit lazy val _oneForge: s.OneForge[AppEffect] =
//      s.OneForge.live[AppStack](forgeConfig)
//
//    implicit lazy val _cache: s.Cache[AppEffect] =
//      s.Cache.caffeineCache[AppStack](cacheConfig)
//
//    implicit lazy val _apply: s.Apply[AppEffect] =
//      s.Apply.monixTask[AppStack]
//
//    val result = p.Rates[AppEffect].get(GetRequest(Currency.USD, Currency.EUR))
//
//    for {
//      _ ← _cache
//        .put(s"${Currency.USD}${Currency.EUR}", rate.asJson.noSpaces)
//        .runAsync
//        .runAsync
//        .map(_.fold(_ ⇒ assert(false), _ ⇒ assert(true)))
//      _ ← result.runAsync.runAsync.map(
//        _.fold(
//          _ ⇒ assert(false),
//          fb ⇒ {
//            fb.pair shouldBe rate.pair
//            fb.price shouldBe rate.price
//            fb.timestamp shouldBe rate.timestamp
//          }
//        )
//      )
//    } yield {
//      assert(true)
//    }
//  }
//
//  it should "get an error when one forge service is offline" in {
//    val forgeConfig =
//      ForgeConfig("api key", "http://test.com", None, None)
//
//    val cacheConfig = CacheConfig(1000, Long.MaxValue nanoseconds)
//
//    implicit val testingBackend =
//      SttpBackendStub[Task, Observable[ByteBuffer], Observable[ByteBuffer]](AsyncHttpClientMonixBackend()).whenAnyRequest
//        .thenRespondServerError()
//
//    implicit lazy val _oneForge: s.OneForge[AppEffect] =
//      s.OneForge.live[AppStack](forgeConfig)
//
//    implicit lazy val _cache: s.Cache[AppEffect] =
//      s.Cache.caffeineCache[AppStack](cacheConfig)
//
//    implicit lazy val _apply: s.Apply[AppEffect] =
//      s.Apply.monixTask[AppStack]
//
//    val result = p.Rates[AppEffect].get(GetRequest(Currency.USD, Currency.EUR))
//
//    for {
//      _ ← result.runAsync.runAsync.map(
//        _.fold(
//          fa ⇒ {
//            fa shouldBe a[ForgeError]
//            fa.asInstanceOf[ForgeError].statusCode shouldBe 500
//          },
//          _ ⇒ assert(false)
//        )
//      )
//      _ ← _cache
//        .get(s"${Currency.USD}${Currency.EUR}")
//        .runAsync
//        .runAsync
//        .map(_.fold(_ ⇒ assert(false), fb ⇒ fb shouldBe None))
//    } yield {
//      assert(true)
//    }
//  }
//}
