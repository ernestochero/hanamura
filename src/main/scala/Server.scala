import akka.http.scaladsl.server.StandardRoute
import sangria.ast.Document
import sangria.execution.{ ErrorWithResolver, Executor, QueryAnalysisError }
import sangria.parser.{ QueryParser, SyntaxError }
import sangria.parser.DeliveryScheme.Try
import sangria.slowlog.SlowLog
import sangria.marshalling.circe._
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.model.MediaTypes._
import akka.http.scaladsl.server._
import akka.stream.ActorMaterializer
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import io.circe._
import io.circe.optics.JsonPath._
import io.circe.parser._
import de.heikoseeberger.akkahttpcirce.ErrorAccumulatingCirceSupport._
import scala.concurrent.duration.{ Duration, SECONDS }
import hanamuraSystem.HanamuraActorSystem._
import hanamuraSystem.HanamuraController

import scala.util.control.NonFatal
import GraphQLRequestUnmarshaller._

import scala.util.{ Failure, Success }

object Server extends App with CorsSupport {
  implicit val timeout      = Timeout(Duration.create(30, SECONDS))
  implicit val materializer = ActorMaterializer()
  implicit val ec           = system.dispatcher

  val config = ConfigFactory.load()
  val host   = config.getString("http.host")
  val port   = config.getInt("http.port")

  def executeGraphQL(
    query: Document,
    operationName: Option[String],
    variables: Option[Json],
    tracing: Boolean
  ): StandardRoute =
    complete(
      Executor
        .execute(
          SchemaDefinition.createSchema,
          query,
          HanamuraController(system),
          variables = variables.getOrElse(Json.obj()),
          operationName = operationName,
          middleware =
            if (tracing) SlowLog.apolloTracing :: Nil
            else Nil
        )
        .map(OK -> _)
        .recover {
          case error: QueryAnalysisError => BadRequest          -> error.resolveError
          case error: ErrorWithResolver  => InternalServerError -> error.resolveError
        }
    )
  def formatError(error: Throwable): Json = error match {
    case syntaxError: SyntaxError =>
      Json.obj(
        "errors" -> Json.arr(
          Json.obj(
            "message" -> Json.fromString(syntaxError.getMessage),
            "locations" -> Json.arr(
              Json.obj("line"   -> Json.fromBigInt(syntaxError.originalError.position.line),
                       "column" -> Json.fromBigInt(syntaxError.originalError.position.column))
            )
          )
        )
      )
    case NonFatal(e) =>
      formatError(e.getMessage)
    case e =>
      throw e
  }

  def formatError(message: String): Json =
    Json.obj("errors" -> Json.arr(Json.obj("message" -> Json.fromString(message))))

  val route: Route =
  optionalHeaderValueByName("X-Apollo-Tracing") { tracing =>
    path("graphql") {
      get {
        explicitlyAccepts(`text/html`) {
          getFromResource("assets/playground.html")
        } ~
        parameters('query, 'operationName.?, 'variables.?) { (query, operationName, variables) =>
          {
            QueryParser.parse(query) match {
              case Success(ast) =>
                variables.map(parse) match {
                  case Some(Left(error)) =>
                    complete(BadRequest, formatError(error))
                  case Some(Right(json)) =>
                    executeGraphQL(ast, operationName, Some(json), tracing.isDefined)
                  case None =>
                    executeGraphQL(ast, operationName, Some(Json.obj()), tracing.isDefined)
                }
              case Failure(error) =>
                complete(BadRequest, formatError(error))
            }
          }
        }
      } ~
      post {
        parameters('query.?, 'operationName.?, 'variables.?) {
          (queryParam, operationNameParam, variablesParam) =>
            {
              entity(as[Json]) { body =>
                val query = queryParam orElse root.query.string.getOption(body)
                val operationName = operationNameParam orElse root.operationName.string
                  .getOption(body)
                val variablesStr = variablesParam orElse root.variables.string.getOption(body)
                query.map(QueryParser.parse(_)) match {
                  case Some(Success(ast)) =>
                    variablesStr.map(parse) match {
                      case Some(Left(error)) => complete(BadRequest, formatError(error))
                      case Some(Right(json)) =>
                        executeGraphQL(ast, operationName, Some(json), tracing.isDefined)
                      case None =>
                        executeGraphQL(ast,
                                       operationName,
                                       root.variables.json.getOption(body),
                                       tracing.isDefined)
                    }
                  case Some(Failure(error)) => complete(BadRequest, formatError(error))
                  case None                 => complete(BadRequest, formatError("No query to execute"))
                }
              }
            }
        }
      }
    }
  } ~
  (get & pathEndOrSingleSlash) {
    redirect("/graphql", PermanentRedirect)
  }

  Http().bindAndHandle(corsHandler(route), host, port)
}