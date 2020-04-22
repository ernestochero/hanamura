package models

final case class MosaicInformation(mosaicId: String, amount: String)
final case class AccountInformation(address: String, mosaics: List[MosaicInformation])
final case class NamespaceInformation(namespaceName: String,
                                      hexadecimal: String,
                                      startHeight: String,
                                      endHeight: String,
                                      expired: Boolean)
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
