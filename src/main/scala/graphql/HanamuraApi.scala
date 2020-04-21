package graphql
import java.math.BigInteger

import scala.language.higherKinds
import caliban.{ GraphQL, RootResolver }
import caliban.schema.{ ArgBuilder, GenericSchema, Schema }
import caliban.GraphQL.graphQL
import graphql.HanamuraService.HanamuraServiceType
import io.nem.symbol.sdk.model.account.Address
import io.nem.symbol.sdk.model.blockchain.BlockDuration
import io.nem.symbol.sdk.model.mosaic.{ MosaicId, MosaicSupplyChangeActionType }
import org.mongodb.scala.bson.ObjectId
import zio.clock.Clock
import zio.console.Console
import symbol.symbolService._
object HanamuraApi extends GenericSchema[HanamuraServiceType with SymbolType] {

  implicit val supplyChangeActionTypeSchema: Schema[Any, MosaicSupplyChangeActionType] =
    Schema
      .gen[models.SupplyActionType]
      .contramap[MosaicSupplyChangeActionType](_.getValue match {
        case 1 => models.SupplyActionType.INCREASE
        case 0 => models.SupplyActionType.DECREASE
        case _ => throw new Exception("Incorrect number to create SupplyActionType")
      })
  implicit val supplyChangeActionTypeArgBuilder: ArgBuilder[MosaicSupplyChangeActionType] =
    ArgBuilder.gen[models.SupplyActionType].map {
      case models.SupplyActionType.INCREASE => MosaicSupplyChangeActionType.INCREASE
      case models.SupplyActionType.DECREASE => MosaicSupplyChangeActionType.DECREASE
      case _ =>
        throw new Exception("Incorrect SupplyActionType to create MosaicSupplyChangeActionType")
    }

  implicit val bigIntegerSchema: Schema[Any, BigInteger] =
    Schema.longSchema.contramap[BigInteger](_.longValue())
  implicit val bigIntegerArgBuilder: ArgBuilder[BigInteger] =
    ArgBuilder.long.map(BigInteger.valueOf)

  implicit val mosaicIdSchema: Schema[Any, MosaicId] =
    Schema.stringSchema.contramap[MosaicId](_.getIdAsHex)
  implicit val mosaicIdArgBuilder: ArgBuilder[MosaicId] =
    ArgBuilder.string.map(new MosaicId(_))

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
          args => HanamuraService.addUser(args.name),
          args =>
            SymbolService.createMosaic(
              args.accountAddress,
              args.blockDuration,
              args.isSupplyMutable,
              args.isTransferable,
              args.isRestrictable,
              args.divisibility,
              args.delta
          ),
          args =>
            SymbolService.modifyMosaicSupply(
              args.accountAddress,
              args.mosaicId,
              args.divisibility,
              args.delta,
              args.supplyChangeActionType
          ),
          args =>
            SymbolService.registerNamespace(args.accountAddress, args.namespaceName, args.duration)
        ),
        Subscriptions(
          HanamuraService.userAddedEvent
        )
      )
    )
}
