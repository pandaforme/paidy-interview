package forex

import cats.effect._
import forex.config._
import forex.domain.OneFrame
import fs2.Stream
import org.http4s.EntityDecoder
import org.http4s.circe.jsonOf
import org.http4s.client.Client
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.server.blaze.BlazeServerBuilder

import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.global

object Main extends IOApp {

  override def run(args: List[String]): IO[ExitCode] = {
    implicit val decoder: EntityDecoder[IO, List[OneFrame]] = jsonOf[IO, List[OneFrame]]
    val resourceClient                                      = BlazeClientBuilder[IO](global).resource

    new Application[IO].stream(executionContext, resourceClient).compile.drain.as(ExitCode.Success)
  }

}

class Application[F[_]: ConcurrentEffect: Timer](implicit oneFrameEntityDecoder: EntityDecoder[F, List[OneFrame]]) {

  def stream(ec: ExecutionContext, resourceClient: Resource[F, Client[F]]): Stream[F, Unit] =
    for {
      config <- Config.stream[F]("app")
      module = new Module[F](config, resourceClient)
      _ <- BlazeServerBuilder[F](ec)
            .bindHttp(config.http.port, config.http.host)
            .withHttpApp(module.httpApp)
            .serve
    } yield {
      ()
    }

}
