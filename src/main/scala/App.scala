import io.nem.sdk.infrastructure.vertx.AccountRepositoryVertxImpl
import io.nem.sdk.model.account.Address
import io.nem.sdk.model.account.Account
import io.nem.sdk.openapi.vertx.invoker.ApiClient
object App {
  def main(args: Array[String]): Unit = {
    //val accountHttp = new AccountRepositoryVertxImpl(new ApiClient())
    val address =
      Address.createFromRawAddress("SAJPSBGA7CWXUMGWIS2JSY6MDG5U26BY4RL65QTS")
    /* val accountInfo = accountHttp.getAccountInfo(address)
    accountInfo.subscribe(in => println(s"information : ${in}"))*/
  }
}
