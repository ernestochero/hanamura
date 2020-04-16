package graphql
import caliban.schema.Annotations.GQLDescription
import graphql.HanamuraService.HanamuraServiceType
import models.User
import zio.ZIO
import symbol.symbolService._

case class idArg(id: String)

case class Queries(
  @GQLDescription("Hanamura say hello to you")
  sayHello: ZIO[HanamuraServiceType, Nothing, String],
  @GQLDescription("Hanamura return all users form database")
  getUsers: ZIO[HanamuraServiceType, Throwable, List[User]],
  @GQLDescription("Hanamura return a user by id")
  getUser: idArg => ZIO[HanamuraServiceType, Throwable, Option[User]],
  @GQLDescription("Hanamura says hello by Symbol")
  getGenerationHashFromBlockGenesis: ZIO[SymbolType, Throwable, String]
)
