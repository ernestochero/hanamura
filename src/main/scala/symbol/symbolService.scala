package symbol
import io.nem.symbol.sdk.api.RepositoryFactory
import io.nem.symbol.sdk.model.account.{ Account, Address }
import zio.{ Has, Queue, Ref, ZIO, ZLayer }
import commons.Transformers._
import io.nem.symbol.sdk.model.blockchain.BlockDuration
import models.HanamuraMessages.{ HanamuraResponse, HanamuraSuccessResponse }
import scala.collection.JavaConverters._
import commons.Constants._
import io.nem.symbol.sdk.model.mosaic.{ MosaicId, MosaicSupplyChangeActionType }
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
      ): ZIO[SymbolType, Throwable, HanamuraResponse]
      def modifyMosaicSupply(
        accountAddress: Address,
        mosaicId: MosaicId,
        divisibility: Int,
        delta: Int,
        supplyChangeActionType: MosaicSupplyChangeActionType
      ): ZIO[SymbolType, Throwable, HanamuraResponse]
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
    ): ZIO[SymbolType, Throwable, HanamuraResponse] =
      ZIO.accessM[SymbolType](
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
    ): ZIO[SymbolType, Throwable, HanamuraResponse] =
      ZIO.accessM[SymbolType](
        _.get.modifyMosaicSupply(
          accountAddress,
          mosaicId,
          divisibility,
          delta,
          supplyChangeActionType
        )
      )

    def make(repositoryFactory: RepositoryFactory): ZLayer[Any, Nothing, Has[Service]] =
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
                  .map(m => models.Mosaic(m.getIdAsHex, m.getAmount.toString))
                  .toList
              } yield models.AccountInformation(address.pretty(), mosaics)

            override def createMosaic(accountAddress: Address,
                                      blockDuration: BlockDuration,
                                      isSupplyMutable: Boolean,
                                      isTransferable: Boolean,
                                      isRestrictable: Boolean,
                                      divisibility: Int,
                                      delta: Int): ZIO[SymbolType, Throwable, HanamuraResponse] =
              for {
                repositoryFactory <- repositoryFactoryRef.get
                networkType       <- repositoryFactory.getNetworkType.toTask
                // find the privateKey on DB with the address account
                privateKey = "291D8F1111DE464C1DACF5CDFA722C104F458C7055D1119078018565EE76626A"
                account    = Account.createFromPrivateKey(privateKey, networkType)
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
            ): ZIO[SymbolType, Throwable, HanamuraResponse] =
              for {
                repositoryFactory <- repositoryFactoryRef.get
                networkType       <- repositoryFactory.getNetworkType.toTask
                // find the privateKey on DB with the address account
                privateKey = "291D8F1111DE464C1DACF5CDFA722C104F458C7055D1119078018565EE76626A"
                account    = Account.createFromPrivateKey(privateKey, networkType)
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
          }
      }
  }
}
