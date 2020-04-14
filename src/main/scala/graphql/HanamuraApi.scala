package graphql
import scala.language.higherKinds
import caliban.{ GraphQL, RootResolver }
import caliban.schema.{ ArgBuilder, GenericSchema, Schema }
import caliban.GraphQL.graphQL
import graphql.HanamuraService.HanamuraServiceType
import org.mongodb.scala.bson.ObjectId
import zio.clock.Clock
import zio.console.Console
object HanamuraApi extends GenericSchema[HanamuraServiceType] {
  implicit val objectIdSchema: Schema[Any, ObjectId] =
    Schema.stringSchema.contramap[ObjectId](_.toHexString)
  implicit val objectIdArgBuilder: ArgBuilder[ObjectId] = ArgBuilder.string.map(new ObjectId(_))
  val api: GraphQL[Console with Clock with HanamuraServiceType] =
    graphQL(
      RootResolver(
        Queries(
          HanamuraService.sayHello,
          HanamuraService.getUsersFromDatabase,
          args => HanamuraService.getUserFromDatabase(args.id),
        ),
        Mutations(
          args => HanamuraService.addUser(args.name)
        ),
        Subscriptions(
          HanamuraService.userAddedEvent
        )
      )
    )
}
