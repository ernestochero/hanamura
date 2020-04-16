package nemservice
import io.nem.symbol.sdk.api._
import io.nem.symbol.sdk.infrastructure.vertx.RepositoryFactoryVertxImpl
import io.nem.symbol.sdk.model.blockchain.BlockInfo
import io.nem.symbol.sdk.model.message.PlainMessage
import io.nem.symbol.sdk.model.mosaic.MosaicId
import commons.Transformers._
import io.nem.symbol.sdk.model.network.NetworkType
import zio.Task

object Factory {
  val repositoryFactory: RepositoryFactory = new RepositoryFactoryVertxImpl(
    "http://localhost:3000"
  )
  val listener: Listener                           = repositoryFactory.createListener()
  val accountRepository: AccountRepository         = repositoryFactory.createAccountRepository()
  val transactionRepository: TransactionRepository = repositoryFactory.createTransactionRepository()
  val blockRepository: BlockRepository             = repositoryFactory.createBlockRepository()
  val mosaicRepository: MosaicRepository           = repositoryFactory.createMosaicRepository()
}

object App {
  import Factory._
  def exec() = {

    val msg = "E2ETest:standaloneTransferTransaction:message-hello world!"
    val blockGenesis: Task[BlockInfo] =
      blockRepository.getBlockByHeight(java.math.BigInteger.valueOf(1)).toFuture.toTask
    // ------------------------------------------------------------------------

    /*    initTransferTransaction(
      externalAccount.getAddress,
      msg,
      myAccount,
      blockGenesis,
      transactionRepository
    )*/

    //createMosaic()
    //transactionInfo()

    /*    def transactionInfo(): Unit = {
      println("#### transaction info ####")
      val eventualResult =
        transactionRepository.getTransactionStatus(
          "25703F250B643993AA952DC1224E1261F1A614A465AB77BA4D2510C7BC6477FC"
        )
      eventualResult.forEach(in => {
        println("transactions detail")
        println(s"### detail : ${in.getStatus}")
      })
    }*/

    /*    def initTransferTransaction(
      recipientAddress: Address,
      message: String,
      fromAccount: Account,
      blockInfo: Observable[BlockInfo],
      transactionRepository: TransactionRepository
    ): Task[TransactionAnnounceResponse] = {
      println("#### init transaction #### ")
      val transferTransactionBuilded = TransferTransactionFactory
        .create(
          fromAccount.getNetworkType,
          recipientAddress,
          Collections.singletonList(NetworkCurrencyMosaic.createAbsolute(BigInteger.valueOf(1))),
          new PlainMessage(message)
        )
        .build()

      val res: Observable[TransactionAnnounceResponse] = blockInfo.flatMap(in => {
        val signedTransaction = fromAccount.sign(transferTransactionBuilded, in.getGenerationHash)
        println(s"# transaction hast : ${signedTransaction.getHash}")
        val transactionAnnounceResponse: Observable[TransactionAnnounceResponse] =
          transactionRepository.announce(signedTransaction)
        transactionAnnounceResponse
      })

      val task = Task.effectSuspendTotal {

        val future = res.toFuture
        if (future.isCancelled)
          Task.fail(new Exception("error"))
        else
          Task.succeed(future.get())
      }
      task
      // res.forEach(in => println(s"result msg from transaction : ${in.getMessage}"))
    }*/
  }

  def main(args: Array[String]): Unit = {
    import zio._
    val blockGenesis: Task[BlockInfo] =
      blockRepository.getBlockByHeight(BigInteger.valueOf(1)).toFuture.toTask
    val runtime = Runtime.default
    //println(runtime.unsafeRunSync(blockGenesis.map(_.getBlockTransactionsHash)))
    createAccountAndShowInformation()
  }
}
