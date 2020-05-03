package graphql

import java.math.BigInteger

import caliban.schema.Annotations.GQLDescription
import graphql.HanamuraService.HanamuraServiceType
import io.nem.symbol.sdk.model.account.Address
import io.nem.symbol.sdk.model.blockchain.BlockDuration
import io.nem.symbol.sdk.model.mosaic.{ MosaicId, MosaicSupplyChangeActionType }
import io.nem.symbol.sdk.model.namespace.AliasAction
import models.HanamuraMessages.HanamuraResponse
import models.User
import symbol.symbolService.SymbolType
import zio.ZIO

case class NameArg(name: String)
case class CreateMosaicArg(
  accountAddress: Address,
  blockDuration: BlockDuration,
  isSupplyMutable: Boolean,
  isTransferable: Boolean,
  isRestrictable: Boolean,
  divisibility: Int,
  delta: Int
)
case class ModifyMosaicSupplyArg(
  accountAddress: Address,
  mosaicId: MosaicId,
  divisibility: Int,
  delta: Int,
  supplyChangeActionType: MosaicSupplyChangeActionType
)

case class RegisterNamespaceArg(accountAddress: Address,
                                namespaceName: String,
                                duration: BigInteger)

case class LinkNamespaceToMosaicArg(
  accountAddress: Address,
  namespaceName: String,
  mosaicId: MosaicId,
  aliasAction: AliasAction
)

case class SendMosaicArg(
  from: Address,
  to: Address,
  mosaicId: MosaicId,
  amount: BigInteger,
  message: String
)

case class Mutations(
  @GQLDescription("Symbol: creates mosaic")
  createMosaic: CreateMosaicArg => ZIO[SymbolType with HanamuraServiceType,
                                       Throwable,
                                       HanamuraResponse],
  @GQLDescription("Symbol: modifies the supply amount of a mosaic")
  modifyMosaicSupply: ModifyMosaicSupplyArg => ZIO[SymbolType with HanamuraServiceType,
                                                   Throwable,
                                                   HanamuraResponse],
  @GQLDescription("Symbol: registers a namespace")
  registerNamespace: RegisterNamespaceArg => ZIO[SymbolType with HanamuraServiceType,
                                                 Throwable,
                                                 HanamuraResponse],
  @GQLDescription("Symbol: links namespace to a mosaic")
  linkNamespaceToMosaic: LinkNamespaceToMosaicArg => ZIO[SymbolType with HanamuraServiceType,
                                                         Throwable,
                                                         HanamuraResponse],
  @GQLDescription("Symbol: sends an amount of mosaic from Address to Another")
  sendMosaic: SendMosaicArg => ZIO[
    SymbolType with HanamuraServiceType,
    Throwable,
    HanamuraResponse
  ]
)
