package testingSymbol
import io.nem.symbol.sdk.api._
import io.nem.symbol.sdk.infrastructure.vertx.RepositoryFactoryVertxImpl
import io.nem.symbol.sdk.model.blockchain.BlockInfo
import io.nem.symbol.sdk.model.message.PlainMessage
import io.nem.symbol.sdk.model.mosaic.MosaicId
import commons.Transformers._
import io.nem.symbol.sdk.model.account.{ AccountInfo, Address }
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

object AccountToTest {
  import Factory._
  // pretty Address : TBKIKY-O7VEPG-7I2XRJ-QG3VIV-B2JOVK-VBAEXZ-W2AB
  val harvestNemesisAccount: Task[AccountInfo] = accountRepository
    .getAccountInfo(
      Address
        .createFromPublicKey("B67370949581A3F6D97A4533665006F5ED05F60D164075EB8614A6DF9D6C39CB",
                             NetworkType.TEST_NET)
    )
    .toTask

  // pretty Address : TBT2F7-F5U3FL-L7K6S3-LBZIGK-J2HM2X-UOT7F7-QIM4
  // privateKey : 291D8F1111DE464C1DACF5CDFA722C104F458C7055D1119078018565EE76626A
  val myAccountToTest: Task[AccountInfo] = accountRepository
    .getAccountInfo(
      Address
        .createFromPublicKey("2948F1862C6BD7A40D0C83B62FEF4278D9FEEDD25BF738AFCE246D532FA8775D",
                             NetworkType.TEST_NET)
    )
    .toTask
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
    import scala.collection.JavaConverters._
    import AccountToTest._
    val runtime = Runtime.default
    println(runtime.unsafeRun(myAccountToTest).getAddress.pretty())
    runtime
      .unsafeRun(myAccountToTest)
      .getMosaics
      .asScala
      .foreach(m => println(s"${m.getAmount} -> ${m.getIdAsHex}"))

  }
}
