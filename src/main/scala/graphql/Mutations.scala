package graphql

import java.math.BigInteger

import caliban.schema.Annotations.GQLDescription
import graphql.HanamuraService.HanamuraServiceType
import io.nem.symbol.sdk.model.account.Address
import io.nem.symbol.sdk.model.blockchain.BlockDuration
import io.nem.symbol.sdk.model.mosaic.{ MosaicId, MosaicSupplyChangeActionType }
import models.HanamuraMessages.HanamuraResponse
import models.User
import symbol.symbolService.SymbolType
import zio.ZIO

case class nameArg(name: String)
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

case class registerNamespaceArg(accountAddress: Address,
                                namespaceName: String,
                                duration: BigInteger)

case class Mutations(
  addUser: nameArg => ZIO[HanamuraServiceType, Throwable, User],
  @GQLDescription("Symbol: create mosaic")
  createMosaic: createMosaicArg => ZIO[SymbolType with HanamuraServiceType,
                                       Throwable,
                                       HanamuraResponse],
  @GQLDescription("Symbol: modify the supply amount of a mosaic")
  modifyMosaicSupply: modifyMosaicSupplyArg => ZIO[SymbolType with HanamuraServiceType,
                                                   Throwable,
                                                   HanamuraResponse],
  @GQLDescription("Symbol: register a namespace")
  registerNamespace: registerNamespaceArg => ZIO[SymbolType with HanamuraServiceType,
                                                 Throwable,
                                                 HanamuraResponse]
)
