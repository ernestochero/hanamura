package Modules
import zio.ZIO
import NemModule._
import io.nem.sdk.infrastructure.vertx.RepositoryFactoryVertxImpl
import io.nem.sdk.model.blockchain.BlockInfo
import nemservice.NemFactory
trait NemModule {
  val nemModule: Service[Any]
}

object NemModule {
  case class NemService(endpoint: String) {
    val repositoryFactory: ZIO[Any, Throwable, RepositoryFactoryVertxImpl] =
      NemFactory.buildRepositoryFactory(endpoint)
    def getBlockGenesis: ZIO[Any, Throwable, BlockInfo] =
      for {
        repoFactory <- repositoryFactory
        blockFactory = repoFactory.createBlockRepository()
        blockGenesis <- NemFactory.getBlockGenesis(blockFactory)
      } yield blockGenesis

  }
  trait Service[R] {
    def nemService(endpoint: String): ZIO[R, Throwable, NemService]
  }

  trait Live extends NemModule {
    override val nemModule: Service[Any] = new Service[Any] {
      override def nemService(endpoint: String): ZIO[Any, Throwable, NemService] =
        ZIO.succeed(NemService(endpoint))
    }
  }

  object factory extends Service[NemModule] {
    override def nemService(endpoint: String): ZIO[NemModule, Throwable, NemService] =
      ZIO.accessM[NemModule](_.nemModule.nemService(endpoint))
  }
}
