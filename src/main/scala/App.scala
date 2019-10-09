import io.nem.sdk.infrastructure.AccountHttp
import io.nem.sdk.model.account.Address
object App {
  def main(args: Array[String]): Unit = {
    val accountHttp = new AccountHttp("http://localhost:3000")
    val address =
      Address.createFromRawAddress("SAJPSBGA7CWXUMGWIS2JSY6MDG5U26BY4RL65QTS")
    val accountInfo = accountHttp.getAccountInfo(address)
    accountInfo.subscribe(in => println(s"information : ${in}"))
  }
}
