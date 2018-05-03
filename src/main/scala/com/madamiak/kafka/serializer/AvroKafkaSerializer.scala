package com.madamiak.kafka.serializer

import java.util

import com.madamiak.kafka.model.Payload
import com.madamiak.kafka.service.SchemaNegotiationService
import org.apache.kafka.common.header.Headers
import org.apache.kafka.common.serialization.ExtendedSerializer
import tech.allegro.schema.json2avro.converter.JsonAvroConverter

import scala.collection.JavaConverters._
import scala.concurrent.duration._
import scala.concurrent.{ Await, ExecutionContext }

/**
  * Using the strain and version performs negotiation with
  * a chosen avro schema registry and serializes the message to the byte array format
  *
  * @param schemaRegistryService service responsible of communicating with the schema registry
  * @param executionContext      program execution context
  */
class AvroKafkaSerializer(
    implicit val schemaRegistryService: SchemaNegotiationService,
    implicit val executionContext: ExecutionContext
) extends ExtendedSerializer[Payload] {

  private val converter = new JsonAvroConverter

  private var negotiationTimeout: Int = 1000

  override def configure(configs: util.Map[String, _], isKey: Boolean = false): Unit =
    negotiationTimeout = configs.asScala
      .getOrElse("registry.negotiation.timeout", 1000)
      .asInstanceOf[Int]

  override def serialize(topic: String, payload: Payload): Array[Byte] =
    throw new UnsupportedOperationException

  override def serialize(topic: String, headers: Headers, payload: Payload): Array[Byte] = {
    headers
      .add("strain", payload.strain.utf8())
      .add("version", payload.version.toString.utf8())

    val bytes = payload match {
      case Payload(strain, version, data) =>
        schemaRegistryService
          .schema(strain, version)
          .map(schema => converter.convertToAvro(data.toString.getBytes(), schema))
    }
    Await.result(bytes, negotiationTimeout.millis)
  }

  override def close(): Unit = {}

}
