package graphql

import models.User
import zio.RIO
import zio.console.Console

case class nameArg(name: String)
case class Mutations(addUser: nameArg => RIO[Console, User])
