package modules
import zio.{ Has, ZIO, ZLayer }
import io.nem.sdk.infrastructure.vertx.RepositoryFactoryVertxImpl
import nemservice.NemFactory
package object nemModule {
  type NemModule = Has[NemModule.Service]
  object NemModule {
    case class NemService(endpoint: String) {
      val repositoryFactory: ZIO[Any, Throwable, RepositoryFactoryVertxImpl] =
        NemFactory.buildRepositoryFactory(endpoint)
      def getGenerationHashFromBlockGenesis: ZIO[Any, Throwable, String] =
        for {
          repoFactory <- repositoryFactory
          blockFactory = repoFactory.createBlockRepository()
          blockGenesis <- NemFactory.getBlockGenesis(blockFactory)
          generationHash = blockGenesis.getGenerationHash
        } yield generationHash
    }
    trait Service {
      def nemService(endpoint: String): ZIO[NemModule, Throwable, NemService]
    }
    val live: ZLayer.NoDeps[Nothing, NemModule] =
      ZLayer.succeed {
        new Service {
          override def nemService(endpoint: String): ZIO[NemModule, Throwable, NemService] =
            ZIO.succeed(NemService(endpoint))
        }
      }
  }
}
