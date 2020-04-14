package modules
import zio.{ Has, ZIO, ZLayer }
import nemservice.NemFactory
package object nemModule {
  type NemModule = Has[NemModule.Service]
  object NemModule {
    trait Service {
      def getGenerationHashFromBlockGenesis(endpoint: String): ZIO[NemModule, Throwable, String]
    }
    val live: ZLayer.NoDeps[Nothing, NemModule] =
      ZLayer.succeed {
        new Service {
          override def getGenerationHashFromBlockGenesis(
            endPoint: String
          ): ZIO[NemModule, Throwable, String] = {
            val repositoryFactory = NemFactory.buildRepositoryFactory(endPoint)
            for {
              repoFactory <- repositoryFactory
              blockFactory = repoFactory.createBlockRepository()
              blockGenesis <- NemFactory.getBlockGenesis(blockFactory)
              generationHash = blockGenesis.getGenerationHash
            } yield generationHash
          }
        }
      }
  }
  def getGenerationHashFromBlockGenesis(endPoint: String): ZIO[NemModule, Throwable, String] =
    ZIO.accessM[NemModule](_.get.getGenerationHashFromBlockGenesis(endPoint))
}
