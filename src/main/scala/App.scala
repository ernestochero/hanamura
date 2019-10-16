import java.math.BigInteger
import java.util.concurrent.TimeUnit

import io.nem.sdk.api.{
  AccountRepository,
  BlockRepository,
  MetadataRepository,
  MosaicRepository,
  NodeRepository,
  RepositoryFactory,
  TransactionRepository
}
import io.nem.sdk.infrastructure.vertx.{ AccountRepositoryVertxImpl, RepositoryFactoryVertxImpl }
import io.nem.sdk.model.account.{ Account, AccountInfo, Address }
import io.nem.sdk.model.blockchain.{ BlockInfo, NetworkType }
import io.nem.sdk.model.mosaic.{ Mosaic, MosaicId, NetworkCurrencyMosaic }
import io.nem.sdk.model.transaction.{
  PlainMessage,
  Transaction,
  TransactionAnnounceResponse,
  TransactionInfo,
  TransferTransaction,
  TransferTransactionFactory,
  UInt64Id
}
import io.reactivex.Observable
import java.util.{ Collections, Optional }

import io.nem.sdk.infrastructure.Listener
object App {
  def main(args: Array[String]): Unit = {
    val privateKey      = "02AD22F0180ED5663435626F1C3A1DEA8745D78AAAF706521EDAA89E53E3E263"
    val externalAccount = Account.createFromPrivateKey(privateKey, NetworkType.MIJIN_TEST)
    val repositoryFactory: RepositoryFactory = new RepositoryFactoryVertxImpl(
      "http://localhost:3001"
    )
    val listener                             = repositoryFactory.createListener()
    val accountRepository: AccountRepository = repositoryFactory.createAccountRepository()
    val transactionRepository                = repositoryFactory.createTransactionRepository()
    val blockRepository: BlockRepository     = repositoryFactory.createBlockRepository()
    val mosaicRepository: MosaicRepository   = repositoryFactory.createMosaicRepository()
    val msg                                  = "E2ETest:standaloneTransferTransaction:message-hello world!"
    val blockGenesis: Observable[BlockInfo] =
      blockRepository.getBlockByHeight(BigInteger.valueOf(1))
    // ------------------------------------------------------------------------
    val myAccount = Account.createFromPrivateKey(
      "47D9FDC9A1B7BF6D53A7F10FCDDF5C951F1FD9DB65E12CE12E6E85B6D76DFCFB",
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
    blockGenesis.forEach(in => println(s"block genesis information ${in.getGenerationHash}"))

    /*    initTransferTransaction(
      externalAccount.getAddress,
      msg,
      myAccount,
      blockGenesis,
      transactionRepository
    )*/

    createMosaic()
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
    ): Observable[TransactionAnnounceResponse] = {
      println("#### init transaction #### ")
      val transferTransactionBuilded = TransferTransactionFactory
        .create(
          fromAccount.getNetworkType,
          recipientAddress,
          Collections.singletonList(NetworkCurrencyMosaic.createAbsolute(BigInteger.valueOf(1))),
          new PlainMessage(message)
        )
        .build()

      val x: Observable[TransactionAnnounceResponse] = blockInfo.flatMap(in => {
        val signedTransaction = fromAccount.sign(transferTransactionBuilded, in.getGenerationHash)
        println(s"# transaction hast : ${signedTransaction.getHash}")
        val transactionAnnounceResponse: Observable[TransactionAnnounceResponse] =
          transactionRepository.announce(signedTransaction)
        transactionAnnounceResponse
      })
      x.forEach(in => println(s"result msg from transaction : ${in.getMessage}"))
      x
    }
  }
}
