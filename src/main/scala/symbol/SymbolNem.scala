package symbol

import java.math.{ BigDecimal, BigInteger }

import io.nem.symbol.sdk.api.{ BlockRepository, NamespaceRepository, TransactionRepository }
import io.nem.symbol.sdk.infrastructure.vertx.RepositoryFactoryVertxImpl
import io.nem.symbol.sdk.model.account.{ Account, UnresolvedAddress }
import io.nem.symbol.sdk.model.blockchain.{ BlockDuration, BlockInfo }
import io.nem.symbol.sdk.model.message.{ Message, PlainMessage }
import io.nem.symbol.sdk.model.mosaic._
import io.nem.symbol.sdk.model.namespace.{ NamespaceId, NamespaceInfo }
import io.nem.symbol.sdk.model.network.NetworkType
import io.nem.symbol.sdk.model.transaction._
import zio.{ Task, ZIO }
import commons.Transformers._
import scala.collection.JavaConverters._

object SymbolNem {
  def buildRepositoryFactory(endPoint: String): Task[RepositoryFactoryVertxImpl] =
    ZIO.effect(new RepositoryFactoryVertxImpl(endPoint))

  def getBlockGenesis(blockRepository: BlockRepository): Task[BlockInfo] =
    blockRepository.getBlockByHeight(BigInteger.valueOf(1)).toFuture.toTask

  def createAccount(implicit networkType: NetworkType): Account =
    Account.generateNewAccount(NetworkType.TEST_NET)

  def createMosaic(
    account: Account,
    blockDuration: BlockDuration,
    isSupplyMutable: Boolean,
    isTransferable: Boolean,
    isRestrictable: Boolean,
    divisibility: Int
  )(implicit networkType: NetworkType): MosaicDefinitionTransaction = {
    val mosaicNonce = MosaicNonce.createRandom()
    MosaicDefinitionTransactionFactory
      .create(
        networkType,
        mosaicNonce,
        MosaicId.createFromNonce(mosaicNonce, account.getPublicAccount),
        MosaicFlags.create(isSupplyMutable, isTransferable, isRestrictable),
        divisibility,
        blockDuration
      )
      .build()
  }

  def mosaicSupplyChangeTransaction(
    mosaicDefinitionTransactionFactory: MosaicDefinitionTransactionFactory,
    delta: Int,
    divisibility: Int
  )(implicit networkType: NetworkType): MosaicSupplyChangeTransaction =
    MosaicSupplyChangeTransactionFactory
      .create(
        networkType,
        mosaicDefinitionTransactionFactory.getMosaicId,
        MosaicSupplyChangeActionType.INCREASE,
        BigDecimal.valueOf(delta * Math.pow(10, divisibility)).toBigInteger
      )
      .build()

  def aggregateTransaction(transactions: List[Transaction], feeAmount: Long)(
    implicit networkType: NetworkType
  ): AggregateTransaction =
    AggregateTransactionFactory
      .createComplete(
        networkType,
        transactions.asJava
      )
      .maxFee(BigInteger.valueOf(feeAmount))
      .build()

  def signTransaction(account: Account,
                      aggregateTransaction: AggregateTransaction,
                      generationHash: String): SignedTransaction =
    account.sign(aggregateTransaction, generationHash)

  def announceTransaction(transactionRepository: TransactionRepository,
                          signedTransaction: SignedTransaction): Task[TransactionAnnounceResponse] =
    transactionRepository.announce(signedTransaction).toFuture.toTask

  def registerNamespace(namespaceName: String,
                        namespaceRepository: NamespaceRepository): Task[NamespaceInfo] = {
    val namespaceId = NamespaceId.createFromName(namespaceName)
    namespaceRepository
      .getNamespace(namespaceId)
      .toFuture
      .toTask
  }

  def registerSubnamespace(subNamespaceName: String, parentId: NamespaceId, fee: BigInteger)(
    implicit networkType: NetworkType
  ): NamespaceRegistrationTransaction =
    NamespaceRegistrationTransactionFactory
      .createSubNamespace(networkType, subNamespaceName, parentId)
      .maxFee(fee)
      .build()

  def buildMosaicToSend(networkCurrencyMosaicId: MosaicId,
                        networkCurrencyDivisibility: Int,
                        amount: BigInteger): Mosaic =
    new Mosaic(networkCurrencyMosaicId,
               amount.multiply(BigInteger.valueOf(10).pow(networkCurrencyDivisibility)))

  def createPlainMessage(payload: String): PlainMessage = PlainMessage.create(payload)

  def sendMosaicsTo(
    recipientAddress: UnresolvedAddress,
    mosaics: List[Mosaic],
    message: Message,
    fee: BigInteger
  )(implicit networkType: NetworkType): TransferTransaction =
    TransferTransactionFactory
      .create(
        networkType,
        recipientAddress,
        mosaics.asJava,
        message
      )
      .maxFee(fee)
      .build()

}
