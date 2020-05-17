package graphql
import caliban.schema.Annotations.GQLDescription
import graphql.HanamuraService.HanamuraServiceType

import zio.stream.ZStream
case class Subscriptions(
  /*  @GQLDescription("Hanamura notify to you when a new user is added")
  userAdded: ZStream[HanamuraServiceType, Nothing, String]*/
)
