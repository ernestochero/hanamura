package models

final case class Mosaic(mosaicId: String, amount: String)
final case class AccountInformation(address: String, mosaics: List[Mosaic])

sealed trait SupplyActionType
object SupplyActionType {
  case object INCREASE extends SupplyActionType
  case object DECREASE extends SupplyActionType
}
