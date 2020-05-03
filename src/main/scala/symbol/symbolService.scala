package symbol
import java.math.BigInteger

import io.nem.symbol.sdk.api.RepositoryFactory
import io.nem.symbol.sdk.model.account.{ Account, Address }
import zio.{ Has, Queue, Ref, ZIO, ZLayer }
import commons.Transformers._
import io.nem.symbol.sdk.model.blockchain.BlockDuration
import models._

import scala.collection.JavaConverters._
import commons.Constants._
import graphql.HanamuraService.HanamuraServiceType
import io.nem.symbol.sdk.model.mosaic.{ Mosaic, MosaicId, MosaicInfo, MosaicSupplyChangeActionType }
import graphql._
import io.nem.symbol.sdk.model.message.PlainMessage
import io.nem.symbol.sdk.model.namespace.{ AliasAction, AliasType, NamespaceId }
import models.HanamuraMessages.{ HanamuraResponse, HanamuraSuccessResponse }

package object symbolService {
  type SymbolType = Has[SymbolService.Service]
  object SymbolService {
    trait Service {
      def getGenerationHashFromBlockGenesis: ZIO[SymbolType, Throwable, String]
      def getAccountInfo(address: Address): ZIO[SymbolType, Throwable, AccountInformation]
      def createMosaic(
        accountAddress: Address,
        blockDuration: BlockDuration,
        isSupplyMutable: Boolean,
        isTransferable: Boolean,
        isRestrictable: Boolean,
        divisibility: Int,
        delta: Int
      ): ZIO[SymbolType with HanamuraServiceType, Throwable, HanamuraResponse]
      def modifyMosaicSupply(
        accountAddress: Address,
        mosaicId: MosaicId,
        divisibility: Int,
        delta: Int,
        supplyChangeActionType: MosaicSupplyChangeActionType
      ): ZIO[SymbolType with HanamuraServiceType, Throwable, HanamuraResponse]

      def registerNamespace(
        accountAddress: Address,
        namespaceName: String,
        duration: BigInteger
      ): ZIO[SymbolType with HanamuraServiceType, Throwable, HanamuraResponse]

      def getNamespaceInfo(
        namespaceName: String
      ): ZIO[SymbolType, Throwable, NamespaceInformation]

      def linkNamespaceToMosaic(
        accountAddress: Address,
        namespaceName: String,
        mosaicId: MosaicId,
        aliasAction: AliasAction
      ): ZIO[SymbolType with HanamuraServiceType, Throwable, HanamuraResponse]

      def getMosaicInfo(
        address: Address,
        mosaicId: MosaicId
      ): ZIO[SymbolType, Throwable, MosaicInformation]

      def sendMosaic(
        from: Address,
        recipientAddress: Address,
        mosaicId: MosaicId,
        amount: BigInteger,
        message: String
      ): ZIO[SymbolType with HanamuraServiceType, Throwable, HanamuraResponse]

    }
    def getGenerationHashFromBlockGenesis: ZIO[SymbolType, Throwable, String] =
      ZIO.accessM[SymbolType](_.get.getGenerationHashFromBlockGenesis)
    def getAccountInfo(address: Address): ZIO[SymbolType, Throwable, AccountInformation] =
      ZIO.accessM[SymbolType](_.get.getAccountInfo(address))
    def createMosaic(
      accountAddress: Address,
      blockDuration: BlockDuration,
      isSupplyMutable: Boolean,
      isTransferable: Boolean,
      isRestrictable: Boolean,
      divisibility: Int,
      delta: Int
    ): ZIO[SymbolType with HanamuraServiceType, Throwable, HanamuraResponse] =
      ZIO.accessM[SymbolType with HanamuraServiceType](
        _.get.createMosaic(
          accountAddress,
          blockDuration,
          isSupplyMutable,
          isTransferable,
          isRestrictable,
          divisibility,
          delta
        )
      )
    def modifyMosaicSupply(
      accountAddress: Address,
      mosaicId: MosaicId,
      divisibility: Int,
      delta: Int,
      supplyChangeActionType: MosaicSupplyChangeActionType
    ): ZIO[SymbolType with HanamuraServiceType, Throwable, HanamuraResponse] =
      ZIO.accessM[SymbolType with HanamuraServiceType](
        _.get.modifyMosaicSupply(
          accountAddress,
          mosaicId,
          divisibility,
          delta,
          supplyChangeActionType
        )
      )

    def registerNamespace(
      accountAddress: Address,
      namespaceName: String,
      duration: BigInteger
    ): ZIO[SymbolType with HanamuraServiceType, Throwable, HanamuraResponse] =
      ZIO.accessM[SymbolType with HanamuraServiceType](
        _.get.registerNamespace(
          accountAddress,
          namespaceName,
          duration
        )
      )

    def getNamespaceInfo(
      namespaceName: String
    ): ZIO[SymbolType, Throwable, NamespaceInformation] =
      ZIO.accessM[SymbolType](_.get.getNamespaceInfo(namespaceName))

    def linkNamespaceToMosaic(
      accountAddress: Address,
      namespaceName: String,
      mosaicId: MosaicId,
      aliasAction: AliasAction
    ): ZIO[SymbolType with HanamuraServiceType, Throwable, HanamuraResponse] =
      ZIO.accessM[SymbolType with HanamuraServiceType](
        _.get.linkNamespaceToMosaic(accountAddress, namespaceName, mosaicId, aliasAction)
      )

    def getMosaicInfo(
      address: Address,
      mosaicId: MosaicId
    ): ZIO[SymbolType, Throwable, MosaicInformation] =
      ZIO.accessM[SymbolType](_.get.getMosaicInfo(address, mosaicId))

    def sendMosaic(
      from: Address,
      recipientAddress: Address,
      mosaicId: MosaicId,
      amount: BigInteger,
      message: String
    ): ZIO[SymbolType with HanamuraServiceType, Throwable, HanamuraResponse] =
      ZIO.accessM[SymbolType with HanamuraServiceType](
        _.get.sendMosaic(
          from,
          recipientAddress,
          mosaicId,
          amount,
          message
        )
      )

    def make(
      repositoryFactory: RepositoryFactory
    ) =
      ZLayer.fromEffect {
        for {
          repositoryFactoryRef <- Ref.make(repositoryFactory)
          subscribers          <- Ref.make(List.empty[Queue[String]])
        } yield
          new Service {
            override def getGenerationHashFromBlockGenesis: ZIO[SymbolType, Throwable, String] =
              for {
                repositoryFactory <- repositoryFactoryRef.get
                blockRepository = repositoryFactory.createBlockRepository()
                blockGenesis <- SymbolNem.getBlockGenesis(blockRepository)
              } yield blockGenesis.getGenerationHash

            override def getAccountInfo(
              address: Address
            ): ZIO[SymbolType, Throwable, AccountInformation] =
              for {
                repositoryFactory <- repositoryFactoryRef.get
                accountRepository   = repositoryFactory.createAccountRepository()
                namespaceRepository = repositoryFactory.createNamespaceRepository()
                accountInfo <- accountRepository.getAccountInfo(address).toTask
                importance = accountInfo.getImportances.asScala.map(_.getValue).toList
                aliases <- SymbolNem.getNamespaceNameFromAccount(address, namespaceRepository)
                mosaics = accountInfo.getMosaics.asScala
                  .map(m => MosaicInformationFromAddress(m.getIdAsHex, m.getAmount))
                  .toList
              } yield
                AccountInformation(accountInfo.getAddress.pretty(),
                                   importance,
                                   accountInfo.getPublicKey,
                                   aliases,
                                   mosaics)

            override def createMosaic(
              accountAddress: Address,
              blockDuration: BlockDuration,
              isSupplyMutable: Boolean,
              isTransferable: Boolean,
              isRestrictable: Boolean,
              divisibility: Int,
              delta: Int
            ): ZIO[SymbolType with HanamuraServiceType, Throwable, HanamuraResponse] =
              for {
                repositoryFactory <- repositoryFactoryRef.get
                networkType       <- repositoryFactory.getNetworkType.toTask
                privateKey        <- HanamuraService.getPrivateKey(accountAddress)
                account = Account.createFromPrivateKey(privateKey, networkType)
                mosaicDefinitionTransaction = SymbolNem.buildMosaicDefinitionTransaction(
                  account,
                  blockDuration,
                  isSupplyMutable,
                  isTransferable,
                  isRestrictable,
                  divisibility,
                  networkType
                )
                mosaicSupplyChangeTransaction = SymbolNem.buildMosaicSupplyChangeTransaction(
                  mosaicDefinitionTransaction,
                  delta,
                  divisibility,
                  networkType
                )
                mosaicTransactions = List(
                  mosaicDefinitionTransaction.toAggregate(account.getPublicAccount),
                  mosaicSupplyChangeTransaction.toAggregate(account.getPublicAccount)
                )
                generationHash <- getGenerationHashFromBlockGenesis
                transaction = SymbolNem.aggregateTransaction(mosaicTransactions,
                                                             defaultFee,
                                                             networkType)
                signedTransaction     = SymbolNem.signTransaction(account, transaction, generationHash)
                transactionRepository = repositoryFactory.createTransactionRepository()
                announcedTransaction <- SymbolNem.announceTransaction(transactionRepository,
                                                                      signedTransaction)
              } yield
                HanamuraSuccessResponse(responseMessage = s"${announcedTransaction.getMessage}")

            override def modifyMosaicSupply(
              accountAddress: Address,
              mosaicId: MosaicId,
              divisibility: Int,
              delta: Int,
              supplyChangeActionType: MosaicSupplyChangeActionType
            ): ZIO[SymbolType with HanamuraServiceType, Throwable, HanamuraResponse] =
              for {
                repositoryFactory <- repositoryFactoryRef.get
                networkType       <- repositoryFactory.getNetworkType.toTask
                privateKey        <- HanamuraService.getPrivateKey(accountAddress)
                account = Account.createFromPrivateKey(privateKey, networkType)
                modifyMosaicSupplyTransaction = SymbolNem.modifyMosaicSupply(
                  mosaicId,
                  divisibility,
                  delta,
                  supplyChangeActionType,
                  networkType
                )
                mosaicTransactions = List(
                  modifyMosaicSupplyTransaction.toAggregate(account.getPublicAccount),
                )
                transaction = SymbolNem.aggregateTransaction(mosaicTransactions,
                                                             defaultFee,
                                                             networkType)
                generationHash <- getGenerationHashFromBlockGenesis
                signedTransaction     = SymbolNem.signTransaction(account, transaction, generationHash)
                transactionRepository = repositoryFactory.createTransactionRepository()
                announcedTransaction <- SymbolNem.announceTransaction(transactionRepository,
                                                                      signedTransaction)
              } yield
                HanamuraSuccessResponse(responseMessage = s"${announcedTransaction.getMessage}")

            override def registerNamespace(
              accountAddress: Address,
              namespaceName: String,
              duration: BigInteger
            ): ZIO[SymbolType with HanamuraServiceType, Throwable, HanamuraResponse] =
              for {
                repositoryFactory <- repositoryFactoryRef.get
                networkType       <- repositoryFactory.getNetworkType.toTask
                privateKey        <- HanamuraService.getPrivateKey(accountAddress)
                account = Account.createFromPrivateKey(privateKey, networkType)
                namespaceRegistrationTransaction = SymbolNem.buildNamespaceRegistrationTransaction(
                  networkType,
                  namespaceName,
                  duration
                )
                namespaceTransactions = List(
                  namespaceRegistrationTransaction.toAggregate(account.getPublicAccount)
                )
                aggregateTransaction = SymbolNem.aggregateTransaction(namespaceTransactions,
                                                                      defaultFee,
                                                                      networkType)
                generationHash <- getGenerationHashFromBlockGenesis
                signedTransaction = SymbolNem.signTransaction(account,
                                                              aggregateTransaction,
                                                              generationHash)
                transactionRepository = repositoryFactory.createTransactionRepository()
                announcedTransaction <- SymbolNem.announceTransaction(transactionRepository,
                                                                      signedTransaction)
              } yield
                HanamuraSuccessResponse(responseMessage = s"${announcedTransaction.getMessage}")

            override def getNamespaceInfo(
              namespaceName: String
            ): ZIO[SymbolType, Throwable, NamespaceInformation] =
              for {
                repositoryFactory <- repositoryFactoryRef.get
                namespaceRepository = repositoryFactory.createNamespaceRepository()
                namespaceId         = NamespaceId.createFromName(namespaceName)
                namespaceInfo <- SymbolNem.getNamespaceInfo(namespaceId, namespaceRepository)
                (aliasType, alias) = SymbolNem.getAliasTypeFromNamespace(namespaceInfo.getAlias)
              } yield
                NamespaceInformation(
                  namespaceName,
                  namespaceInfo.getMetaId,
                  namespaceInfo.getStartHeight.toString,
                  namespaceInfo.getEndHeight.toString,
                  namespaceInfo.isExpired,
                  aliasType,
                  alias
                )

            override def linkNamespaceToMosaic(
              address: Address,
              namespaceName: String,
              mosaicId: MosaicId,
              aliasAction: AliasAction
            ): ZIO[SymbolType with HanamuraServiceType, Throwable, HanamuraResponse] =
              for {
                repositoryFactory <- repositoryFactoryRef.get
                networkType       <- repositoryFactory.getNetworkType.toTask
                privateKey        <- HanamuraService.getPrivateKey(address)
                account     = Account.createFromPrivateKey(privateKey, networkType)
                namespaceId = NamespaceId.createFromName(namespaceName)
                mosaicAliasTransaction = SymbolNem.buildMosaicAliasTransaction(networkType,
                                                                               namespaceId,
                                                                               mosaicId,
                                                                               aliasAction)
                transactions = List(
                  mosaicAliasTransaction.toAggregate(account.getPublicAccount)
                )
                aggregateTransaction = SymbolNem.aggregateTransaction(transactions,
                                                                      defaultFee,
                                                                      networkType)
                generationHash <- getGenerationHashFromBlockGenesis
                signedTransaction = SymbolNem.signTransaction(account,
                                                              aggregateTransaction,
                                                              generationHash)
                transactionRepository = repositoryFactory.createTransactionRepository()
                announcedTransaction <- SymbolNem.announceTransaction(transactionRepository,
                                                                      signedTransaction)

              } yield
                HanamuraSuccessResponse(responseMessage = s"${announcedTransaction.getMessage}")

            override def getMosaicInfo(
              address: Address,
              mosaicId: MosaicId
            ): ZIO[SymbolType, Throwable, MosaicInformation] =
              for {
                repositoryFactory <- repositoryFactoryRef.get
                accountRepository   = repositoryFactory.createAccountRepository()
                mosaicRepository    = repositoryFactory.createMosaicRepository()
                namespaceRepository = repositoryFactory.createNamespaceRepository()
                namespaceName <- SymbolNem.getNamespaceNameFromMosaicId(mosaicId,
                                                                        namespaceRepository)
                mosaicInfo <- mosaicRepository.getMosaic(mosaicId).toTask
                account    <- accountRepository.getAccountInfo(address).toTask
                mosaic = account.getMosaics.asScala
                  .find(_.getIdAsHex == mosaicId.getIdAsHex)
              } yield
                MosaicInformation(
                  mosaicInfo.getMosaicId.getIdAsHex,
                  namespaceName.map(_.getName),
                  mosaicInfo.getSupply.toString,
                  mosaic.map(_.getAmount),
                  mosaicInfo.getDivisibility,
                  mosaicInfo.isTransferable,
                  mosaicInfo.isSupplyMutable,
                  mosaicInfo.isTransferable
                )

            override def sendMosaic(
              from: Address,
              recipientAddress: Address,
              mosaicId: MosaicId,
              amount: BigInteger,
              message: String
            ): ZIO[SymbolType with HanamuraServiceType, Throwable, HanamuraResponse] =
              for {
                repositoryFactory <- repositoryFactoryRef.get
                networkType       <- repositoryFactory.getNetworkType.toTask
                privateKey        <- HanamuraService.getPrivateKey(from)
                account = Account.createFromPrivateKey(privateKey, networkType)
                mosaicInfo <- getMosaicInfo(from, mosaicId)
                mosaic = new Mosaic(
                  mosaicId,
                  SymbolNem.calculateAbsoluteAmount(amount, mosaicInfo.divisibility)
                )
                transferTransaction = SymbolNem.buildTransferTransaction(
                  recipientAddress,
                  List(mosaic),
                  PlainMessage.create(message),
                  networkType
                )
                transactions = List(
                  transferTransaction.toAggregate(account.getPublicAccount)
                )
                aggregateTransaction = SymbolNem.aggregateTransaction(
                  transactions,
                  defaultFee,
                  networkType
                )
                generationHash <- getGenerationHashFromBlockGenesis
                signedTransaction = SymbolNem.signTransaction(
                  account,
                  aggregateTransaction,
                  generationHash
                )
                transactionRepository = repositoryFactory.createTransactionRepository()
                announcedTransaction <- SymbolNem.announceTransaction(transactionRepository,
                                                                      signedTransaction)

              } yield
                HanamuraSuccessResponse(
                  responseMessage = s"${announcedTransaction.getMessage}"
                )
          }
      }
  }
}
