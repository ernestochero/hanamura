package graphql
import caliban.schema.Annotations.GQLDescription
import models.User
import zio.{ Task, URIO }
import zio.console.Console
case class Queries(
  @GQLDescription("Hanamura say hello to you")
  sayHello: () => Task[String],
  /*  @GQLDescription("Hanamura return all users form database")
  getUsers: () => Task[Seq[User]]*/
)
