package dev.madamiak.kafka.service

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, HttpResponse, StatusCodes}
import akka.stream.ActorMaterializer
import com.github.benmanes.caffeine.cache.{Caffeine, Cache => CCache}
import com.typesafe.config.ConfigFactory.load
import dev.madamiak.kafka.model.Version
import org.apache.avro.Schema
import org.apache.avro.Schema.Parser
import spray.json._

import scala.concurrent.{ExecutionContext, Future}
import scalacache._
import scalacache.caffeine._
import scalacache.memoization._
import scalacache.modes.scalaFuture._

/**
  * Default schema registry service. Responsible for acquiring a schema from a chosen avro schema registry.
  * Utilizes a cache to minimize the number of calls between the serializer and schema registry.
  * This service is able to resolve the Avro schema only when the registry does follow REST API convention e.g.:
  * http://schema-registry:30000/schema/generic/1.2.0
  *
  * @param system       underlying actor system
  * @param context      program execution context
  * @param materializer actor materializer
  */
class DefaultSchemaRegistryHttpService(
                                        implicit val system: ActorSystem,
                                        implicit val context: ExecutionContext,
                                        implicit val materializer: ActorMaterializer
) extends SchemaRegistryService {

  private val underlyingCache: CCache[String, Entry[Schema]] = Caffeine.newBuilder()
    .maximumSize(load().getInt("registry.schema.cache.size"))
    .build[String, Entry[Schema]]

  private implicit val scalaCache: CaffeineCache[Schema] = CaffeineCache(underlyingCache)

  def schema(strain: String, version: Version): Future[Schema] = memoizeF(Some(load().getDuration("registry.schema.cache.expiration"))) {
    for {
      response <- request(strain, version)
      schema <- response.status match {
        case StatusCodes.OK => response.entity.toStrict(load().getDuration("registry.schema.negotiation.timeout"))
          .map { e =>
            e.data
              .utf8String
              .parseJson
              .asJsObject
              .getFields("schema")
              .mkString
          }
        case _              => throw new SchemaNegotiationException
      }
    } yield new Parser().parse(schema)
  }

  def request(strain: String, version: Version): Future[HttpResponse] =
    Http().singleRequest(HttpRequest(
      uri = s"http://${load().getString("registry.host")}:${load().getInt("registry.port")}/${load().getString("registry.path")}/$strain/${version.toString}"
    ))

}
