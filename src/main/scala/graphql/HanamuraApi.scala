package graphql
import scala.language.higherKinds
import caliban.{ GraphQL, RootResolver }
import caliban.schema.{ ArgBuilder, GenericSchema, Schema }
import caliban.GraphQL.graphQL
import graphql.HanamuraService.HanamuraServiceType
import org.mongodb.scala.bson.ObjectId
import zio.clock.Clock
import zio.console.Console
import symbol.symbolService._
object HanamuraApi extends GenericSchema[HanamuraServiceType with SymbolType] {
  implicit val objectIdSchema: Schema[Any, ObjectId] =
    Schema.stringSchema.contramap[ObjectId](_.toHexString)
  implicit val objectIdArgBuilder: ArgBuilder[ObjectId] = ArgBuilder.string.map(new ObjectId(_))
  val api: GraphQL[Console with Clock with HanamuraServiceType with SymbolType] =
    graphQL(
      RootResolver(
        Queries(
          HanamuraService.sayHello,
          HanamuraService.getUsersFromDatabase,
          args => HanamuraService.getUserFromDatabase(args.id),
          SymbolService.getGenerationHashFromBlockGenesis
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
