package graphql
import caliban.schema.Annotations.GQLDescription
import graphql.HanamuraService.HanamuraServiceType
import models.User
import zio.{ RIO, Task, UIO, ZIO }
import zio.console.Console

case class idArg(id: String)

case class Queries(
  @GQLDescription("Hanamura say hello to you")
  sayHello: ZIO[HanamuraServiceType, Nothing, String],
  @GQLDescription("Hanamura return all users form database")
  getUsers: ZIO[HanamuraServiceType, Throwable, List[User]],
  @GQLDescription("Hanamura return a user by id")
  getUser: idArg => ZIO[HanamuraServiceType, Throwable, Option[User]],
)

/*
@GQLDescription("Hanamura return a blockGenesis from BlockChain")
getBlockGenesis: ZIO[Any, Throwable, String]*/
