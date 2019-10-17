import akka.stream.Materializer
import akka.util.Timeout

import sangria.schema._
import sangria.macros.derive._
import scala.concurrent.ExecutionContext
import hanamuraSystem.HanamuraController
object SchemaDefinition {
  def createSchema(
    implicit timeout: Timeout,
    ec: ExecutionContext,
    mat: Materializer
  ): Schema[HanamuraController, Unit] = {
    val miscellaneousFieldQueries = fields[HanamuraController, Unit](
      Field(
        "SayHelloHanamura",
        StringType,
        description = Some("hanamura says hello to you"),
        resolve = context => {
          context.ctx.sayHello
        }
      )
    )

    val QueryType = ObjectType("Query", miscellaneousFieldQueries)

    Schema(QueryType)
  }
}
