package configuration

import pureconfig.ConfigSource
import zio.{ Has, Layer, ZIO, ZLayer }
import pureconfig.generic.auto._
package object configurationService {
  type ConfigurationModule = Has[ConfigurationModule.Service]
  object ConfigurationModule {
    case class ConfigurationError(message: String) extends RuntimeException(message)
    case class HttpConf(host: String, port: Int)
    case class MongoConf(database: String, uri: String, userCollection: String)
    case class Configuration(appName: String, httpConf: HttpConf, mongoConf: MongoConf)
    trait Service {
      def buildConfiguration: ZIO[ConfigurationModule, Throwable, Configuration]
    }
    val live: Layer[Nothing, ConfigurationModule] =
      ZLayer.succeed {
        new Service {
          override def buildConfiguration: ZIO[ConfigurationModule, Throwable, Configuration] =
            ZIO
              .fromEither(
                ConfigSource.default.load[Configuration]
              )
              .mapError(e => ConfigurationError(e.toList.mkString(", ")))
        }
      }
  }
  def buildConfiguration: ZIO[ConfigurationModule, Throwable, ConfigurationModule.Configuration] =
    ZIO.accessM[ConfigurationModule](_.get.buildConfiguration)
}
