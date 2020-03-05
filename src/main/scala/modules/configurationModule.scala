package modules
import pureconfig._
import pureconfig.generic.auto._
import zio.{ Has, ZIO, ZLayer }
package object configurationModule {
  type ConfigurationModule = Has[ConfigurationModule.Service]
  object ConfigurationModule {
    case class ConfigurationError(message: String) extends RuntimeException(message)
    case class HttpConf(host: String, port: Int)
    case class MongoConf(database: String, uri: String, userCollection: String)
    case class Configuration(appName: String, httpConf: HttpConf, mongoConf: MongoConf)
    trait Service {
      def configuration: ZIO[ConfigurationModule, Throwable, Configuration]
    }
    val live: ZLayer.NoDeps[Nothing, ConfigurationModule] =
      ZLayer.succeed {
        new Service {
          override def configuration: ZIO[ConfigurationModule, Throwable, Configuration] =
            ZIO
              .fromEither(
                ConfigSource.default.load[Configuration]
              )
              .mapError(e => ConfigurationError(e.toList.mkString(", ")))
        }
      }
  }
}
