package graphql
import zio.ZIO
import pureconfig._
import pureconfig.generic.auto._
import ConfigurationModule._
trait ConfigurationModule {
  val configurationModule: Service[Any]
}
object ConfigurationModule {
  case class ConfigurationError(message: String) extends RuntimeException(message)
  case class Http(host: String, port: Int)
  case class Configuration(appName: String, http: Http)

  trait Service[R] {
    def configuration: ZIO[R, Throwable, Configuration]
  }

  trait Live extends ConfigurationModule {
    override val configurationModule: Service[Any] = new Service[Any] {
      override def configuration: ZIO[Any, Throwable, Configuration] =
        ZIO
          .fromEither(
            ConfigSource.default.load[Configuration]
          )
          .mapError(e => ConfigurationError(e.toList.mkString(", ")))
    }
  }

  object factory extends Service[ConfigurationModule] {
    override def configuration: ZIO[ConfigurationModule, Throwable, Configuration] =
      ZIO.accessM[ConfigurationModule](_.configurationModule.configuration)
  }
}
