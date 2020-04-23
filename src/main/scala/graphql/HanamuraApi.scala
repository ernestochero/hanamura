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
import io.nem.symbol.sdk.model.namespace.AliasAction
import models._
import org.mongodb.scala.bson.ObjectId
import zio.clock.Clock
import zio.console.Console
import symbol.symbolService._
object HanamuraApi extends GenericSchema[HanamuraServiceType with SymbolType] {

  implicit val aliasActionSchema: Schema[Any, AliasAction] =
    Schema
      .gen[AliasActionType]
      .contramap[AliasAction](_.getValue.toInt match {
        case 1 => AliasActionType.LINK
        case 0 => AliasActionType.UNLINK
        case _ => throw new Exception("Incorrect number to create AliasActionType")
      })

  implicit val aliasActionSchemaArgBuilder: ArgBuilder[AliasAction] =
    ArgBuilder.gen[AliasActionType].map {
      case AliasActionType.LINK   => AliasAction.LINK
      case AliasActionType.UNLINK => AliasAction.UNLINK
      case _ =>
        throw new Exception("Incorrect SupplyActionType to create AliasAction")
    }

  implicit val supplyChangeActionTypeSchema: Schema[Any, MosaicSupplyChangeActionType] =
    Schema
      .gen[SupplyActionType]
      .contramap[MosaicSupplyChangeActionType](_.getValue match {
        case 1 => SupplyActionType.INCREASE
        case 0 => SupplyActionType.DECREASE
        case _ => throw new Exception("Incorrect number to create SupplyActionType")
      })
  implicit val supplyChangeActionTypeArgBuilder: ArgBuilder[MosaicSupplyChangeActionType] =
    ArgBuilder.gen[SupplyActionType].map {
      case SupplyActionType.INCREASE => MosaicSupplyChangeActionType.INCREASE
      case SupplyActionType.DECREASE => MosaicSupplyChangeActionType.DECREASE
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
          args => SymbolService.getAccountInfo(args.address),
          args => SymbolService.getNamespaceInfo(args.namespaceName),
          args => SymbolService.getMosaicInfo(args.mosaicId)
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
            SymbolService.registerNamespace(args.accountAddress, args.namespaceName, args.duration),
          args =>
            SymbolService.linkNamespaceToMosaic(args.accountAddress,
                                                args.namespaceName,
                                                args.mosaicId,
                                                args.aliasAction)
        ),
        Subscriptions(
          HanamuraService.userAddedEvent
        )
      )
    )
}
