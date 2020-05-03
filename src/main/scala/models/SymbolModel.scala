package models
import java.math.BigInteger

final case class MosaicInformation(mosaicId: String,
                                   nameSpaceName: Option[String] = None,
                                   supply: String,
                                   balance: Option[BigInteger],
                                   divisibility: Int,
                                   transferable: Boolean,
                                   mutable: Boolean,
                                   restrictable: Boolean)

final case class AccountInformation(
  address: String,
  importances: List[BigInteger],
  publicKey: String,
  aliases: List[String],
  mosaics: List[MosaicInformationFromAddress]
)

final case class NamespaceInformation(namespaceName: String,
                                      hexadecimal: String,
                                      startHeight: String,
                                      endHeight: String,
                                      expired: Boolean,
                                      aliasType: String,
                                      alias: String)
final case class MosaicInformationFromAddress(
  IdAsHex: String,
  amount: BigInteger
)
sealed trait AliasActionType
object AliasActionType {
  case object LINK   extends AliasActionType
  case object UNLINK extends AliasActionType
}

sealed trait SupplyActionType
object SupplyActionType {
  case object INCREASE extends SupplyActionType
  case object DECREASE extends SupplyActionType
}
