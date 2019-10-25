package graphql
import caliban.schema.Annotations.GQLDescription
import models.User
import zio.{ RIO, Task }
import zio.console.Console

case class idArg(id: String)

case class Queries(
  @GQLDescription("Hanamura say hello to you")
  sayHello: RIO[Console, String],
  @GQLDescription("Hanamura return all users form database")
  getUsers: Task[List[User]],
  @GQLDescription("Hanamura return a user by id")
  getUser: idArg => RIO[Console, Option[User]]
)
