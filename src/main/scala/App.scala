import java.math.BigInteger

import io.nem.sdk.api.{
  AccountRepository,
  BlockRepository,
  MetadataRepository,
  NodeRepository,
  RepositoryFactory,
  TransactionRepository
}
import io.nem.sdk.infrastructure.vertx.{ AccountRepositoryVertxImpl, RepositoryFactoryVertxImpl }
import io.nem.sdk.model.account.{ Account, AccountInfo, Address }
import io.nem.sdk.model.blockchain.{ BlockInfo, NetworkType }
import io.nem.sdk.model.mosaic.{ Mosaic, NetworkCurrencyMosaic }
import io.nem.sdk.model.transaction.{
  PlainMessage,
  Transaction,
  TransactionAnnounceResponse,
  TransactionInfo,
  TransferTransaction,
  TransferTransactionFactory
}
import io.nem.sdk.openapi
import io.nem.sdk.openapi.vertx.invoker.ApiClient
import io.reactivex.Observable
import java.util.{ Collections, Optional }

import io.nem.sdk.infrastructure.Listener
object App {
  def main(args: Array[String]): Unit = {
    val privateKey      = "9092486B067AAE2E7EDF139840410C2F7D64906D3B8CF46CB5DF8AF1323D97FE"
    val externalAccount = Account.createFromPrivateKey(privateKey, NetworkType.MIJIN_TEST)
    val myAccount       = Account.generateNewAccount(NetworkType.MIJIN_TEST)
    println(
      s"Your new account address is: ${myAccount.getAddress.pretty()} and it's private key: ${myAccount.getPrivateKey}"
    )
    println(
      s"Your new account address is: ${externalAccount.getAddress.pretty()} and it's private key: ${externalAccount.getPrivateKey}"
    )
    val repositoryFactory: RepositoryFactory = new RepositoryFactoryVertxImpl(
      "http://localhost:3001"
    )
    val nodeRepository: NodeRepository = repositoryFactory.createNodeRepository()
    println(s"Node info")
    val friendlyName: Observable[NetworkType] =
      nodeRepository.getNodeInfo.map(in => in.getNetworkIdentifier)
    friendlyName.forEach(c => println(c.getValue))

    println(s"Block info")
    val blockRepository: BlockRepository = repositoryFactory.createBlockRepository()
    val blockInfo: Observable[BlockInfo] = blockRepository.getBlockByHeight(BigInteger.valueOf(1))
    blockInfo.forEach(in => println(s"iii ${in.getGenerationHash}"))

    val transactionRepository = repositoryFactory.createTransactionRepository()

    val accountRepository: AccountRepository = repositoryFactory.createAccountRepository()
    val accountInfo: Observable[AccountInfo] =
      accountRepository.getAccountInfo(externalAccount.getAddress)

    accountInfo.subscribe(in => {
      val mosaics = in.getMosaics
      println("show account info")
      println(s"___ ${in.getAddressHeight} ___")
      mosaics.forEach(m => println(s"_ Mosaic : ${m.getIdAsHex} _ "))
    })

    // listener
    val listener = repositoryFactory.createListener()

    // init Transfer Transaction
    val msg = "E2ETest:standaloneTransferTransaction:message-hello world!"

    def initTransferTransaction(
      recipientAddress: Address,
      message: String,
      fromAccount: Account,
      blockInfo: Observable[BlockInfo],
      transactionRepository: TransactionRepository
    ): Observable[TransactionAnnounceResponse] = {
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
        val transactionAnnounceResponse: Observable[TransactionAnnounceResponse] =
          transactionRepository.announce(signedTransaction)
        transactionAnnounceResponse
      })
      x
    }

    def validateTransactionAnnounceCorrectly(address: Address,
                                             transactionHash: String,
                                             listener: Listener) = {
      val observableTransaction = listener
        .confirmed(address)
        .filter(t => {
          t.getTransactionInfo()
            .flatMap[String](_.getHash)
            .filter(_ == transactionHash)
            .isPresent
        })
    }
    /*
    def getTransactionOrFail(address: Address,
                             listener: Listener,
                             observable: Observable[Transaction]) = {
      val errorOrTransactionObservable = Observable.merge()
    }*/

  }
}
