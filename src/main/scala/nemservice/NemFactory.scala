package nemservice

import java.math.BigInteger

import io.nem.sdk.api._
import io.nem.sdk.infrastructure.vertx.RepositoryFactoryVertxImpl
import commons.Transformers._
import io.nem.sdk.model.blockchain.BlockInfo
import zio.{ Task, ZIO }

object NemFactory {
  private def validateEndpoint: Boolean = true
  def buildRepositoryFactory(endPoint: String): ZIO[Any, Throwable, RepositoryFactoryVertxImpl] =
    if (validateEndpoint)
      Task.succeed(new RepositoryFactoryVertxImpl(endPoint))
    else
      Task.fail(new Exception("Failed to validateEndpoint"))
  def getBlockGenesis(blockRepository: BlockRepository): Task[BlockInfo] =
    blockRepository.getBlockByHeight(BigInteger.valueOf(1)).toFuture.toTask
}
