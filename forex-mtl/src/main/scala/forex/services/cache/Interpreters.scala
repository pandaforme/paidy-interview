package forex.services.cache

import cats.Applicative
import cats.effect.Concurrent
import com.github.blemale.scaffeine
import com.github.blemale.scaffeine.Scaffeine
import forex.config.CacheConfig
import forex.services.cache.interpreters.{ CaffeineCache, MemoryCache }

import scala.collection.mutable

object Cache {
  def memoryCache: mutable.Map[String, String] = scala.collection.mutable.Map.empty

  def caffeineCache(cacheConfig: CacheConfig): scaffeine.Cache[String, String] =
    Scaffeine()
      .recordStats()
      .expireAfterWrite(cacheConfig.timeToLive)
      .maximumSize(cacheConfig.maximumSize.value)
      .build[String, String]()
}

object Interpreters {
  def memoryCache[F[_]: Applicative]: Algebra[F] =
    MemoryCache(Cache.memoryCache)

  def caffeineCache[F[_]: Concurrent](cacheConfig: forex.config.CacheConfig): Algebra[F] =
    CaffeineCache(Cache.caffeineCache(cacheConfig))
}
