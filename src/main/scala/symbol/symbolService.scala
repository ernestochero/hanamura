package symbol
import io.nem.symbol.sdk.api.RepositoryFactory
import io.nem.symbol.sdk.model.account.{ Account, Address }
import zio.{ Has, Queue, Ref, ZIO, ZLayer }
import commons.Transformers._
import io.nem.symbol.sdk.model.blockchain.BlockDuration
import io.nem.symbol.sdk.model.network.NetworkType
import models.HanamuraMessages.{ HanamuraResponse, HanamuraSuccessResponse }

import scala.collection.JavaConverters._

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
    }
    def getGenerationHashFromBlockGenesis: ZIO[SymbolType, Throwable, String] =
      ZIO.accessM[SymbolType](_.get.getGenerationHashFromBlockGenesis)
    def getAccountInfo(address: Address): ZIO[SymbolType, Throwable, models.AccountInformation] =
      ZIO.accessM[SymbolType](_.get.getAccountInfo(address))

    def make(repositoryFactory: RepositoryFactory): ZLayer[Any, Nothing, Has[Service]] =
      ZLayer.fromEffect {
        for {
          networkType          <- repositoryFactory.getNetworkType.toTask
          repositoryFactoryRef <- Ref.make(repositoryFactory)
          subscribers          <- Ref.make(List.empty[Queue[String]])
        } yield
          new Service {
            implicit val implicitNetworkType: NetworkType = networkType
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
                privateKey = "291D8F1111DE464C1DACF5CDFA722C104F458C7055D1119078018565EE76626A"
                account    = Account.createFromPrivateKey(privateKey, networkType)
                mosaicTransaction = SymbolNem.createMosaicTransaction(
                  account,
                  blockDuration,
                  isSupplyMutable,
                  isTransferable,
                  isRestrictable,
                  divisibility,
                  delta
                )
                generationHash <- getGenerationHashFromBlockGenesis
                transaction           = SymbolNem.aggregateTransaction(List(mosaicTransaction), 1000)
                signedTransaction     = SymbolNem.signTransaction(account, transaction, generationHash)
                transactionRepository = repositoryFactory.createTransactionRepository()
                announcedTransaction <- SymbolNem.announceTransaction(transactionRepository,
                                                                      signedTransaction)
              } yield {
                HanamuraSuccessResponse(responseMessage = s"${announcedTransaction.getMessage}")
              }

          }
      }
  }
}
