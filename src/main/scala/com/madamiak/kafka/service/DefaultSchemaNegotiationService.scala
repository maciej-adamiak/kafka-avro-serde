package com.madamiak.kafka.service

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, HttpResponse, StatusCodes}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import com.github.benmanes.caffeine.cache.{Caffeine, Cache => CCache}
import com.madamiak.kafka.model.Version
import com.typesafe.config.{Config, ConfigFactory}
import org.apache.avro.Schema
import org.apache.avro.Schema.Parser
import scalacache._
import scalacache.caffeine._
import scalacache.memoization._
import scalacache.modes.scalaFuture._
import spray.json._

import scala.concurrent.{ExecutionContext, Future}

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
class DefaultSchemaNegotiationService(
    implicit val system: ActorSystem,
    implicit val context: ExecutionContext,
    implicit val materializer: ActorMaterializer
) extends SchemaNegotiationService {

  private val config: Config = ConfigFactory.load()

  private val underlyingCache: CCache[String, Entry[Schema]] = Caffeine
    .newBuilder()
    .maximumSize(config.getInt("registry.schema.cache.size"))
    .build[String, Entry[Schema]]

  private implicit val scalaCache: CaffeineCache[Schema] = CaffeineCache(underlyingCache)

  def schema(strain: String, version: Version): Future[Schema] =
    memoizeF(Some(config.getDuration("registry.schema.cache.expiration"))) {

      request(strain, version)
        .flatMap(
          response =>
            response.status match {
              case StatusCodes.OK => Unmarshal(response.entity).to[String]
              case _              => Future.failed(new SchemaNegotiationException)
          }
        )
        .map(
          _.parseJson.asJsObject
            .getFields("schema")
            .mkString
        )
        .map(x => new Parser().parse(x))
    }

  def request(strain: String, version: Version): Future[HttpResponse] =
    Http().singleRequest(
      HttpRequest(
        uri = s"http://${config.getString("registry.host")}:${config.getInt("registry.port")}/${config
          .getString("registry.path")}/$strain/${version.toString}"
      )
    )

}
