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
  // pretty Address : TAB32R-QVKKYU-5HH52N-7ZIUDT-5X2OS2-KYXLBQ-NQRK
  // privateKey : 0186348C54DCFA1873218997F79BCBDC3D9684514CB1DD5E8103C45CE78F25D3
  val otherAccountToTest: Task[AccountInfo] = accountRepository
    .getAccountInfo(
      Address
        .createFromPublicKey("37D2D793FFECDF868A7AD7FD0EBCA39702FB4355200B7F8078A7455ABA60CF48",
                             NetworkType.TEST_NET)
    )
    .toTask

  // pretty Address : TBKXHX-2DZDBE-KYFU3D-V4X2HT-5G4QLX-KKVUYN-EITK
  // privateKey : 1D0F91CA18292A324AA8E50A37383C73BDEE7866F6A1F465FA82841CB82C7A2E
  val myAccountToTest: Task[AccountInfo] = accountRepository
    .getAccountInfo(
      Address
        .createFromPublicKey("2628FC775E0D2C3C484EB9663FB0E6BABEB7F41154F46745DB78D6F189F99758",
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

    println(runtime.unsafeRun(otherAccountToTest).getAddress.pretty())
    runtime
      .unsafeRun(otherAccountToTest)
      .getMosaics
      .asScala
      .foreach(m => println(s"${m.getAmount} -> ${m.getIdAsHex}"))

  }
}
