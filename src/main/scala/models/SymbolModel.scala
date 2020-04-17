package models

final case class Mosaic(mosaicId: String, amount: String)
final case class AccountInformation(address: String, mosaics: List[Mosaic])
