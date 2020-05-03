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
import configuration.configurationService._
import symbol.symbolService._
import zio.blocking.Blocking
import cats.effect.Blocker
import zio._
import symbol.SymbolNem
import commons.Logger.logger
object HanamuraServer extends CatsApp with GenericSchema[Console with Clock] {
  type HanamuraTask[A] = RIO[ZEnv, A]
  val symbolHost                                                    = "http://localhost:3000"
  override def run(args: List[String]): ZIO[zio.ZEnv, Nothing, Int] = program
  val logic: ZIO[zio.ZEnv with ConfigurationModule, Nothing, Int] = (for {
    conf <- buildConfiguration
    userCollection <- Mongo.setupMongoConfiguration[User](
      conf.mongoConf.uri,
      conf.mongoConf.database,
      conf.mongoConf.userCollection
    )
    _ = logger.info(s" > Initializing program ${conf.appName} ")
    repositoryFactory <- SymbolNem.buildRepositoryFactory(symbolHost)
    symbolLayer = SymbolService.make(repositoryFactory)
    _ <- HanamuraService
      .make(userCollection)
      .memoize
      .use(
        layer =>
          for {
            _ <- ZIO
              .access[Blocking](_.get.blockingExecutor.asEC)
              .map(Blocker.liftExecutionContext)
            interpreter <- HanamuraApi.api.interpreter
              .map(_.provideCustomLayer(layer ++ symbolLayer))
            _ <- BlazeServerBuilder[HanamuraTask]
              .bindHttp(conf.httpConf.port, conf.httpConf.host)
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

  val liveEnvironments = zio.ZEnv.live ++ ConfigurationModule.live
  private val program  = logic.provideLayer(liveEnvironments)
}
