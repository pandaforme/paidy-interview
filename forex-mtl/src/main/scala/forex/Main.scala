package forex

import cats.effect._
import forex.config._
import forex.services.cache.{ Interpreters => CacheServiceInterpreters }
import forex.services.rates.{ Interpreters => RatesServiceInterpreters }
import forex.services.{ CacheService, RatesService }
import fs2.Stream
import org.http4s.server.blaze.BlazeServerBuilder

import scala.concurrent.ExecutionContext

object Main extends IOApp {

  override def run(args: List[String]): IO[ExitCode] = {
    val config: ApplicationConfig               = Config.stream[IO]("app").compile.drain.asInstanceOf[ApplicationConfig]
    implicit val ratesService: RatesService[IO] = RatesServiceInterpreters.live(config.oneFrame)
    implicit val cacheService: CacheService[IO] = CacheServiceInterpreters.caffeineCache(config.cache)

    new Application[IO].stream(executionContext)(config).compile.drain.as(ExitCode.Success)
  }

}

class Application[F[_]: ConcurrentEffect: Timer: RatesService: CacheService] {

  def stream(ec: ExecutionContext): ApplicationConfig => Stream[F, Unit] =
    config => {
      val module: Module[F] = new Module[F](config)
      BlazeServerBuilder[F](ec)
        .bindHttp(config.http.port, config.http.host)
        .withHttpApp(module.httpApp)
        .serve
        .map(_ => ())
    }
}
