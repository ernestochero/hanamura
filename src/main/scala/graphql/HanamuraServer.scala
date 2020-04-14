package graphql
import caliban.schema.GenericSchema
import caliban.Http4sAdapter
import mongodb.Mongo
import org.http4s.implicits._
import org.http4s.server.Router
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.middleware.CORS
import zio.{ RIO, ZIO }
import zio.clock.Clock
import zio.console.{ Console, putStrLn }
import zio.interop.catz._
import models.User
import modules.configurationModule.ConfigurationModule
import modules.nemModule.NemModule
import zio.blocking.Blocking
import cats.effect.Blocker
import zio._
object HanamuraServer extends CatsApp with GenericSchema[Console with Clock] {
  type HanamuraTask[A] = RIO[ZEnv, A]
  override def run(args: List[String]): ZIO[zio.ZEnv, Nothing, Int] = program
  val logic: ZIO[zio.ZEnv with NemModule with ConfigurationModule, Nothing, Int] = (for {
    configuration <- modules.configurationModule.configuration
    userCollection <- Mongo.setupMongoConfiguration[User](
      configuration.mongoConf.uri,
      configuration.mongoConf.database,
      configuration.mongoConf.userCollection
    )
    nemService <- modules.nemModule.getGenerationHashFromBlockGenesis("http://54.187.97.142:3000")
    _ = println(s"Block $nemService")
    _ <- HanamuraService
      .make(userCollection)
      .memoize
      .use(
        layer =>
          for {
            _ <- ZIO
              .access[Blocking](_.get.blockingExecutor.asEC)
              .map(Blocker.liftExecutionContext)
            interpreter <- HanamuraApi.api.interpreter.map(_.provideCustomLayer(layer))
            _ <- BlazeServerBuilder[HanamuraTask]
              .bindHttp(configuration.httpConf.port, configuration.httpConf.host)
              .withHttpApp(
                Router[HanamuraTask](
                  "/api/graphql" -> CORS(Http4sAdapter.makeHttpService(interpreter)),
                  "/ws/graphql"  -> CORS(Http4sAdapter.makeWebSocketService(interpreter)),
                ).orNotFound
              )
              .resource
              .toManaged
              .useForever
          } yield 0
      )
  } yield 0).catchAll(err => putStrLn(err.toString).as(1))

  val liveEnvironments = zio.ZEnv.live ++ ConfigurationModule.live ++ NemModule.live
  private val program  = logic.provideLayer(liveEnvironments)
}
