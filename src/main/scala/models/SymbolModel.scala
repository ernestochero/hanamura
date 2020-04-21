package models

final case class Mosaic(mosaicId: String, amount: String)
final case class AccountInformation(address: String, mosaics: List[Mosaic])
final case class NamespaceInformation(namespaceName: String,
                                      hexadecimal: String,
                                      startHeight: String,
                                      endHeight: String,
                                      expired: Boolean)
sealed trait SupplyActionType
object SupplyActionType {
  case object INCREASE extends SupplyActionType
  case object DECREASE extends SupplyActionType
}
