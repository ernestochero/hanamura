package graphql
import caliban.schema.Annotations.GQLDescription
import graphql.HanamuraService.HanamuraServiceType
import io.nem.symbol.sdk.model.account.Address
import io.nem.symbol.sdk.model.mosaic.MosaicId
import models._
import zio.ZIO
import symbol.symbolService._

case class idArg(id: String)
case class getNamespaceInfoArg(namespaceName: String)
case class addressArg(address: Address)
case class getMosaicInfoArg(address: Address, mosaicId: MosaicId)
case class Queries(
  @GQLDescription("Hanamura say hello to you")
  sayHello: ZIO[HanamuraServiceType, Nothing, String],
  @GQLDescription("Hanamura return all users form database")
  getUsers: ZIO[HanamuraServiceType, Throwable, List[User]],
  @GQLDescription("Hanamura return a user by id")
  getUser: idArg => ZIO[HanamuraServiceType, Throwable, Option[User]],
  @GQLDescription("Hanamura says hello by Symbol")
  getGenerationHashFromBlockGenesis: ZIO[SymbolType, Throwable, String],
  @GQLDescription("Symbol: Get AccountInfo from raw address")
  getAccountInfo: addressArg => ZIO[SymbolType, Throwable, AccountInformation],
  @GQLDescription("Symbol: Get namespaceInfo from namespace name")
  getNamespaceInfo: getNamespaceInfoArg => ZIO[SymbolType, Throwable, NamespaceInformation],
  @GQLDescription("Symbol: Get namespaceInfo from namespace name")
  getMosaicInfo: getMosaicInfoArg => ZIO[SymbolType, Throwable, MosaicInformation]
)
