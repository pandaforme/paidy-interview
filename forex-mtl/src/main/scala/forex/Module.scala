package forex

import cats.effect.{ Concurrent, Resource, Timer }
import forex.config.ApplicationConfig
import forex.domain.OneFrame
import forex.http.rates.RatesHttpRoutes
import forex.programs._
import forex.services._
import forex.services.cache.{ Interpreters => CacheServiceInterpreters }
import forex.services.rates.{ Interpreters => RatesServiceInterpreters }
import org.http4s._
import org.http4s.client.Client
import org.http4s.implicits._
import org.http4s.server.middleware.{ AutoSlash, Timeout }

class Module[F[_]: Concurrent: Timer](config: ApplicationConfig)(
    implicit oneFrameEntityDecoder: EntityDecoder[F, List[OneFrame]],
    resourceClient: Resource[F, Client[F]]
) {
  implicit val ratesService: RatesService[F] = RatesServiceInterpreters.live(config.oneFrame)
  implicit val cacheService: CacheService[F] = CacheServiceInterpreters.caffeineCache(config.cache)

  private val ratesProgram: RatesProgram[F] = RatesProgram[F]

  private val ratesHttpRoutes: HttpRoutes[F] = new RatesHttpRoutes[F](ratesProgram).routes

  type PartialMiddleware = HttpRoutes[F] => HttpRoutes[F]
  type TotalMiddleware   = HttpApp[F] => HttpApp[F]

  private val routesMiddleware: PartialMiddleware = {
    { http: HttpRoutes[F] =>
      AutoSlash(http)
    }
  }

  private val appMiddleware: TotalMiddleware = { http: HttpApp[F] =>
    Timeout(config.http.timeout)(http)
  }

  private val http: HttpRoutes[F] = ratesHttpRoutes

  val httpApp: HttpApp[F] = appMiddleware(routesMiddleware(http).orNotFound)

}
