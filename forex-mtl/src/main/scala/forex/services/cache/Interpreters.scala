package forex.services.cache

import cats.Id
import cats.effect.IO
import com.github.blemale.scaffeine.Scaffeine
import forex.services.cache.interpreters.{ CaffeineCache, MemoryCache }
import eu.timepit.refined.auto._

object Interpreters {
  def mapCache: Algebra[Id] = MemoryCache()

  def caffeineCache(cacheConfig: forex.config.CacheConfig): Algebra[IO] = {
    val caffeine = Scaffeine()
      .recordStats()
      .expireAfterWrite(cacheConfig.timeToLive)
      .maximumSize(cacheConfig.maximumSize)
      .build[String, String]()
    CaffeineCache(caffeine)
  }
}
