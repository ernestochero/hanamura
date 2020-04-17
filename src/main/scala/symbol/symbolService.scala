package symbol
import io.nem.symbol.sdk.api.RepositoryFactory
import io.nem.symbol.sdk.model.account.Address
import zio.{ Has, Queue, Ref, ZIO, ZLayer }
import commons.Transformers._
import scala.collection.JavaConverters._

package object symbolService {
  type SymbolType = Has[SymbolService.Service]
  object SymbolService {
    trait Service {
      def getGenerationHashFromBlockGenesis: ZIO[SymbolType, Throwable, String]
      def getAccountInfo(address: Address): ZIO[SymbolType, Throwable, models.AccountInformation]
    }
    def getGenerationHashFromBlockGenesis: ZIO[SymbolType, Throwable, String] =
      ZIO.accessM[SymbolType](_.get.getGenerationHashFromBlockGenesis)
    def getAccountInfo(address: Address): ZIO[SymbolType, Throwable, models.AccountInformation] =
      ZIO.accessM[SymbolType](_.get.getAccountInfo(address))

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
          }
      }
  }
}
