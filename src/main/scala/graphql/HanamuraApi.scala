package graphql
import scala.language.higherKinds
import caliban.{ GraphQL, RootResolver }
import caliban.schema.{ ArgBuilder, GenericSchema, Schema }
import caliban.GraphQL.graphQL
import graphql.HanamuraService.HanamuraServiceType
import io.nem.symbol.sdk.model.account.{ AccountInfo, Address }
import io.nem.symbol.sdk.model.blockchain.BlockDuration
import org.mongodb.scala.bson.ObjectId
import zio.clock.Clock
import zio.console.Console
import symbol.symbolService._
object HanamuraApi extends GenericSchema[HanamuraServiceType with SymbolType] {

  implicit val blockDurationSchema: Schema[Any, BlockDuration] =
    Schema.longSchema.contramap[BlockDuration](_.getDuration)
  implicit val blockDurationArgBuilder: ArgBuilder[BlockDuration] =
    ArgBuilder.long.map(new BlockDuration(_))

  implicit val addressIdSchema: Schema[Any, Address] =
    Schema.stringSchema.contramap[Address](_.pretty())
  implicit val addressArgBuilder: ArgBuilder[Address] =
    ArgBuilder.string.map(Address.createFromRawAddress)

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
          SymbolService.getGenerationHashFromBlockGenesis,
          args => SymbolService.getAccountInfo(args.address)
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
