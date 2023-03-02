package forex.services.rates

import cats.Applicative
import cats.effect.{ Concurrent, Resource, Timer }
import forex.config.OneFrameConfig
import forex.domain.OneFrame
import forex.services.rates.interpreters._
import org.http4s.EntityDecoder
import org.http4s.client.Client

object Interpreters {

  def dummy[F[_]: Applicative]: Algebra[F] = new OneFrameDummy[F]()
  def live[F[_]: Concurrent: Timer](forgeConfig: OneFrameConfig)(
      implicit oneFrameEntityDecoder: EntityDecoder[F, List[OneFrame]],
      resourceClient: Resource[F, Client[F]]
  ): Algebra[F] =
    OneFrameLive(forgeConfig)

}
