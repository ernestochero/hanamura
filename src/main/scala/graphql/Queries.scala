package graphql
import caliban.schema.Annotations.GQLDescription
import graphql.HanamuraService.HanamuraServiceType
import io.nem.symbol.sdk.model.account.Address
import io.nem.symbol.sdk.model.blockchain.BlockDuration
import io.nem.symbol.sdk.model.mosaic.{ MosaicId, MosaicSupplyChangeActionType }
import models.HanamuraMessages.HanamuraResponse
import models.User
import zio.ZIO
import symbol.symbolService._

case class idArg(id: String)
case class addressArg(address: Address)
case class createMosaicArg(
  accountAddress: Address,
  blockDuration: BlockDuration,
  isSupplyMutable: Boolean,
  isTransferable: Boolean,
  isRestrictable: Boolean,
  divisibility: Int,
  delta: Int
)
case class modifyMosaicSupplyArg(
  accountAddress: Address,
  mosaicId: MosaicId,
  divisibility: Int,
  delta: Int,
  supplyChangeActionType: MosaicSupplyChangeActionType
)

case class Queries(
  @GQLDescription("Hanamura say hello to you")
  sayHello: ZIO[HanamuraServiceType, Nothing, String],
  @GQLDescription("Hanamura return all users form database")
  getUsers: ZIO[HanamuraServiceType, Throwable, List[User]],
  @GQLDescription("Hanamura return a user by id")
  getUser: idArg => ZIO[HanamuraServiceType, Throwable, Option[User]],
  @GQLDescription("Hanamura says hello by Symbol")
  getGenerationHashFromBlockGenesis: ZIO[SymbolType, Throwable, String],
  @GQLDescription("Symbol: Get AccountInfo by raw address")
  getAccountInfo: addressArg => ZIO[SymbolType, Throwable, models.AccountInformation],
  @GQLDescription("Symbol: create mosaic")
  createMosaic: createMosaicArg => ZIO[SymbolType with HanamuraServiceType,
                                       Throwable,
                                       HanamuraResponse],
  @GQLDescription("Symbol: modify the supply amount of a mosaic")
  modifyMosaicSupply: modifyMosaicSupplyArg => ZIO[SymbolType with HanamuraServiceType,
                                                   Throwable,
                                                   HanamuraResponse]
)
