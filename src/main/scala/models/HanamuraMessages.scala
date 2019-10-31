package models

object HanamuraMessages {

  abstract class FieldId(val name: String) extends Serializable {
    override def toString: String = name
  }

  case object UserField extends FieldId("user")

  sealed trait HanamuraMessage {
    def message: String
  }

  sealed trait HanamuraResponse extends HanamuraMessage {
    val responseCode: String
    val responseMessage: String
    override def message: String =
      s"ResponseCode : $responseCode with message : $responseMessage"
  }

  def getSuccessGetMessage(fieldId: FieldId): String =
    s"The ${fieldId.name} was extracted successfully"

  case class HanamuraSuccessResponse(responseCode: String = "00", responseMessage: String)
      extends HanamuraResponse

  case class HanamuraFailedResponse(responseCode: String = "01", responseMessage: String)
      extends HanamuraResponse

  case class HanamuraGetUsersResponse(userDomain: Option[Seq[User]],
                                      hanamuraResponse: HanamuraResponse)
}
