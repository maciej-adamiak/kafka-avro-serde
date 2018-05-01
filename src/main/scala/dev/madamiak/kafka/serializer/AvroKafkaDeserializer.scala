package dev.madamiak.kafka.serializer

import java.util

import dev.madamiak.kafka.model.{ Payload, Version }
import dev.madamiak.kafka.service.SchemaRegistryService
import org.apache.kafka.common.header.Headers
import org.apache.kafka.common.serialization.ExtendedDeserializer
import spray.json._
import tech.allegro.schema.json2avro.converter.JsonAvroConverter

import scala.collection.JavaConverters._
import scala.concurrent.duration._
import scala.concurrent.{ Await, ExecutionContext }

/**
  * Using the strain and version message headers performs negotiation with
  * a chosen avro schema registry and deserializes the message to the Payload format
  *
  * @param schemaRegistryService service responsible of communicating with the schema registry
  * @param executionContext      program execution context
  */
class AvroKafkaDeserializer(
    implicit val schemaRegistryService: SchemaRegistryService,
    implicit val executionContext: ExecutionContext
) extends ExtendedDeserializer[Payload] {

  private val converter = new JsonAvroConverter

  private var negotiationTimeout: Int = 300

  override def configure(configs: util.Map[String, _], isKey: Boolean): Unit =
    negotiationTimeout = configs.asScala.getOrElse("registry.negotiation.timeout", 300).asInstanceOf[Int]

  override def close(): Unit = {}

  override def deserialize(topic: String, headers: Headers, data: Array[Byte]): Payload = {
    val strain  = headers.lastHeader("strain").value().utf8()
    val version = Version(headers.lastHeader("version").value().utf8())

    val payload = schemaRegistryService
      .schema(strain, version)
      .map(schema => converter.convertToJson(data, schema))
      .map(raw => Payload(strain, version, raw.utf8().parseJson.asJsObject))

    Await.result(payload, negotiationTimeout.millis)
  }

  override def deserialize(topic: String, data: Array[Byte]): Payload =
    throw new UnsupportedOperationException
}
