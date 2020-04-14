package graphql

import graphql.HanamuraService.HanamuraServiceType
import models.User
import zio.ZIO

case class nameArg(name: String)
case class Mutations(addUser: nameArg => ZIO[HanamuraServiceType, Throwable, User])
