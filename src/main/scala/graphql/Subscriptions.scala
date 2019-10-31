package graphql
import caliban.schema.Annotations.GQLDescription
import zio.console.Console
import zio.stream.ZStream
case class Subscriptions(
  @GQLDescription("Hanamura notify to you when a new user is added")
  userAdded: ZStream[Console, Nothing, String]
)
