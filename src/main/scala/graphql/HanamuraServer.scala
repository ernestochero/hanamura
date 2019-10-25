package graphql
import caliban.schema.{ ArgBuilder, GenericSchema, Schema }
import caliban.GraphQL._
import caliban.RootResolver
import com.typesafe.config.ConfigFactory
import mongodb.Mongo
import org.http4s.implicits._
import org.http4s.server.Router
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.middleware.CORS
import org.mongodb.scala.bson.ObjectId
import zio.{ RIO, ZIO }
import zio.clock.Clock
import zio.console.{ Console, putStrLn }
import zio.interop.catz._
import caliban.Http4sAdapter
object HanamuraServer extends CatsApp with GenericSchema[Console with Clock] {
  val config = ConfigFactory.load()
  val host   = config.getString("http.host")
  val port   = config.getInt("http.port")
  type HanamuraTask[A] = RIO[Console with Clock, A]
  case class DatabaseConnection(conn: String = "test connection")
  implicit val objectIdSchema     = Schema.stringSchema.contramap[ObjectId](_.toHexString)
  implicit val objectIdArgBuilder = ArgBuilder.string.map(new ObjectId(_))
  override def run(args: List[String]): ZIO[zio.ZEnv, Nothing, Int] =
    (for {
      service <- HanamuraService.make(Mongo.usersCollection)
      interpreter = graphQL(
        RootResolver(
          Queries(service.sayHello, service.getUserFromDatabase),
          Mutations(args => service.addUser(args.name))
        )
      )
      _ <- BlazeServerBuilder[HanamuraTask]
        .bindHttp(port, host)
        .withHttpApp(
          Router(
            "/api/graphql" -> CORS(Http4sAdapter.makeRestService(interpreter)),
            "/ws/graphql"  -> CORS(Http4sAdapter.makeWebSocketService(interpreter))
          ).orNotFound
        )
        .resource
        .toManaged
        .useForever
    } yield 0).catchAll(err => putStrLn(err.toString).as(1))
}
