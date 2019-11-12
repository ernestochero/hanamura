package nemservice

import java.math.BigInteger
import java.util.Collections

import io.nem.sdk.api._
import io.nem.sdk.infrastructure.vertx.RepositoryFactoryVertxImpl
import io.nem.sdk.model.account.{ Account, Address }
import io.nem.sdk.model.blockchain.{ BlockInfo, NetworkType }
import io.nem.sdk.model.message.PlainMessage
import io.nem.sdk.model.mosaic.{ MosaicId, NetworkCurrencyMosaic }
import io.nem.sdk.model.transaction.{ TransactionAnnounceResponse, TransferTransactionFactory }
import io.reactivex.Observable
import commons.Transformers._
import io.nem.sdk.infrastructure.Listener
import zio.{ DefaultRuntime, Task }

object Factory {
  val repositoryFactory: RepositoryFactory = new RepositoryFactoryVertxImpl(
    "http://103.3.60.174:3000"
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
    val privateKey      = "02AD22F0180ED5663435626F1C3A1DEA8745D78AAAF706521EDAA89E53E3E263"
    val externalAccount = Account.createFromPrivateKey(privateKey, NetworkType.MIJIN_TEST)
    val msg             = "E2ETest:standaloneTransferTransaction:message-hello world!"
    val blockGenesis: Task[BlockInfo] =
      blockRepository.getBlockByHeight(BigInteger.valueOf(1)).toFuture.toTask
    // ------------------------------------------------------------------------
    val myAccount = Account.createFromPrivateKey(
      "25B3F54217340F7061D02676C4B928ADB4395EB70A2A52D2A11E2F4AE011B03E",
      NetworkType.MIJIN_TEST
    )

    println(
      s"my account address is: ${myAccount.getAddress.pretty()}\n" +
      s"private key: ${myAccount.getPrivateKey}\n" +
      s"public key : ${myAccount.getPublicKey}"
    )
    println(
      s"external account address is: ${externalAccount.getAddress.pretty()}\n " +
      s"private key: ${externalAccount.getPrivateKey}\n" +
      s"public key : ${externalAccount.getPublicKey}"
    )

    /*    initTransferTransaction(
      externalAccount.getAddress,
      msg,
      myAccount,
      blockGenesis,
      transactionRepository
    )*/

    //createMosaic()
    //transactionInfo()

    def transactionInfo(): Unit = {
      println("#### transaction info ####")
      val eventualResult =
        transactionRepository.getTransactionStatus(
          "25703F250B643993AA952DC1224E1261F1A614A465AB77BA4D2510C7BC6477FC"
        )
      eventualResult.forEach(in => {
        println("transactions detail")
        println(s"### detail : ${in.getStatus}")
      })
    }

    def createMosaic(): Unit = {
      println("#### creating mosaics ####")
      val mosaic = mosaicRepository.getMosaic(new MosaicId("4B2E2871E0614525"))
      mosaic.forEach(in => {
        println(s"### detail ${in.getMosaicId}")
      })

    }

    def initTransferTransaction(
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
    }
  }
  /*
  def main(args: Array[String]): Unit = {
    val blockGenesis: Task[BlockInfo] =
      blockRepository.getBlockByHeight(BigInteger.valueOf(1)).toFuture.toTask

    val runtime = new DefaultRuntime {}
    println(runtime.unsafeRunSync(blockGenesis.map(_.getBlockTransactionsHash)))
  }*/
}
