package symbol
import io.nem.symbol.sdk.api.RepositoryFactory
import zio.{ Has, Queue, Ref, ZIO, ZLayer }

package object symbolService {
  type SymbolType = Has[SymbolService.Service]
  object SymbolService {
    trait Service {
      def getGenerationHashFromBlockGenesis: ZIO[SymbolType, Throwable, String]
    }
    def getGenerationHashFromBlockGenesis: ZIO[SymbolType, Throwable, String] =
      ZIO.accessM[SymbolType](_.get.getGenerationHashFromBlockGenesis)

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
          }
      }

  }
}
