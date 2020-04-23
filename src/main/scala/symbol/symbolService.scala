package symbol
import java.math.BigInteger

import io.nem.symbol.sdk.api.RepositoryFactory
import io.nem.symbol.sdk.model.account.{ Account, Address }
import zio.{ Has, Queue, Ref, ZIO, ZLayer }
import commons.Transformers._
import io.nem.symbol.sdk.model.blockchain.BlockDuration
import models.HanamuraMessages.{ HanamuraResponse, HanamuraSuccessResponse }

import scala.collection.JavaConverters._
import commons.Constants._
import graphql.HanamuraService.HanamuraServiceType
import io.nem.symbol.sdk.model.mosaic.{ MosaicId, MosaicInfo, MosaicSupplyChangeActionType }
import graphql._
import io.nem.symbol.sdk.model.namespace.{ AliasAction, NamespaceId }
import models.MosaicInformation
package object symbolService {
  type SymbolType = Has[SymbolService.Service]
  object SymbolService {
    trait Service {
      def getGenerationHashFromBlockGenesis: ZIO[SymbolType, Throwable, String]
      def getAccountInfo(address: Address): ZIO[SymbolType, Throwable, models.AccountInformation]
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
      ): ZIO[SymbolType, Throwable, models.NamespaceInformation]

      def linkNamespaceToMosaic(
        accountAddress: Address,
        namespaceName: String,
        mosaicId: MosaicId,
        aliasAction: AliasAction
      ): ZIO[SymbolType with HanamuraServiceType, Throwable, HanamuraResponse]

      def getMosaicInfo(
        mosaicId: MosaicId
      ): ZIO[SymbolType, Throwable, MosaicInformation]

    }
    def getGenerationHashFromBlockGenesis: ZIO[SymbolType, Throwable, String] =
      ZIO.accessM[SymbolType](_.get.getGenerationHashFromBlockGenesis)
    def getAccountInfo(address: Address): ZIO[SymbolType, Throwable, models.AccountInformation] =
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
    ): ZIO[SymbolType, Throwable, models.NamespaceInformation] =
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
      mosaicId: MosaicId
    ): ZIO[SymbolType, Throwable, MosaicInformation] =
      ZIO.accessM[SymbolType](_.get.getMosaicInfo(mosaicId))

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
            ): ZIO[SymbolType, Throwable, models.AccountInformation] =
              for {
                repositoryFactory <- repositoryFactoryRef.get
                accountRepository = repositoryFactory.createAccountRepository()
                accountInfo <- accountRepository.getAccountInfo(address).toTask
                mosaics = accountInfo.getMosaics.asScala
                  .map(m => models.MosaicInformation(m.getIdAsHex, m.getAmount.toString))
                  .toList
              } yield models.AccountInformation(address.pretty(), mosaics)

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
                                                             mosaicFee,
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
                                                             mosaicFee,
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
                                                                      mosaicFee,
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
            ): ZIO[SymbolType, Throwable, models.NamespaceInformation] =
              for {
                repositoryFactory <- repositoryFactoryRef.get
                namespaceRepository = repositoryFactory.createNamespaceRepository()
                namespaceId         = NamespaceId.createFromName(namespaceName)
                namespaceInfo <- SymbolNem.getNamespaceInfo(namespaceId, namespaceRepository)
              } yield
                models.NamespaceInformation(namespaceName,
                                            namespaceInfo.getMetaId,
                                            namespaceInfo.getStartHeight.toString,
                                            namespaceInfo.getEndHeight.toString,
                                            namespaceInfo.isExpired)

            override def linkNamespaceToMosaic(
              accountAddress: Address,
              namespaceName: String,
              mosaicId: MosaicId,
              aliasAction: AliasAction
            ): ZIO[SymbolType with HanamuraServiceType, Throwable, HanamuraResponse] =
              for {
                repositoryFactory <- repositoryFactoryRef.get
                networkType       <- repositoryFactory.getNetworkType.toTask
                privateKey        <- HanamuraService.getPrivateKey(accountAddress)
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
                                                                      mosaicFee,
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
              mosaicId: MosaicId
            ): ZIO[SymbolType, Throwable, MosaicInformation] =
              for {
                repositoryFactory <- repositoryFactoryRef.get
                mosaicRepository    = repositoryFactory.createMosaicRepository()
                namespaceRepository = repositoryFactory.createNamespaceRepository()
                namespaceName <- SymbolNem.getNamespaceNameFromMosaicId(mosaicId,
                                                                        namespaceRepository)
                mosaicInfo <- mosaicRepository.getMosaic(mosaicId).toTask
              } yield
                MosaicInformation(mosaicInfo.getMosaicId.getIdAsHex,
                                  mosaicInfo.getSupply.toString,
                                  namespaceName.map(_.getName))
          }
      }
  }
}
