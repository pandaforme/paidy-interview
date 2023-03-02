package forex.config

import eu.timepit.refined.api.Refined
import eu.timepit.refined.numeric.Positive
import eu.timepit.refined.string.Url

import scala.concurrent.duration.FiniteDuration

case class ApplicationConfig(
    http: HttpConfig,
    oneFrame: OneFrameConfig,
    cache: CacheConfig
)

case class HttpConfig(
    host: String,
    port: Int,
    timeout: FiniteDuration
)

final case class CacheConfig(maximumSize: Long Refined Positive, timeToLive: FiniteDuration)

final case class OneFrameConfig(key: String,
                                host: String Refined Url,
                                maxWait: FiniteDuration,
                                maxRetry: Int Refined Positive)
