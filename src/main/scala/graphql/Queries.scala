package graphql
import caliban.schema.Annotations.GQLDescription
import zio.URIO
import zio.console.Console
case class Queries(
  @GQLDescription("Hanamura say hello to you")
  sayHello: () => URIO[Console, String]
)
