package graphql
import caliban.schema.Annotations.GQLDescription
import graphql.HanamuraService.HanamuraServiceType
import io.nem.symbol.sdk.model.account.{ Account, Address }
import io.nem.symbol.sdk.model.blockchain.BlockDuration
import models.HanamuraMessages.HanamuraResponse
import models.User
import zio.ZIO
import symbol.symbolService._

case class idArg(id: String)
case class addressArg(address: Address)
case class mosaicCreationArg(
  accountAddress: Address,
  blockDuration: BlockDuration,
  isSupplyMutable: Boolean,
  isTransferable: Boolean,
  isRestrictable: Boolean,
  divisibility: Int,
  delta: Int
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
  createMosaic: mosaicCreationArg => ZIO[SymbolType, Throwable, HanamuraResponse]
)
