package dev.madamiak.kafka.serializer

import java.util

import dev.madamiak.kafka.model.Payload
import dev.madamiak.kafka.service.SchemaRegistryService
import org.apache.kafka.common.serialization._

import scala.concurrent.ExecutionContext

/**
  * Wraps both avro serializer and deserializer
  *
  * @param schemaRegistryService service responsible of communicating with the schema registry
  * @param executionContext      program execution context
  */
class AvroKafkaSerde(
  implicit val schemaRegistryService: SchemaRegistryService,
  implicit val executionContext: ExecutionContext
) extends Serde[Payload] {

  private val avroKafkaSerializer = new AvroKafkaSerializer()
  private val avroKafkaDeserializer = new AvroKafkaDeserializer()

  override def configure(configs: util.Map[String, _], isKey: Boolean): Unit = {
    avroKafkaSerializer.configure(configs, isKey)
    avroKafkaDeserializer.configure(configs, isKey)
  }

  override def close(): Unit = {}

  override def serializer(): ExtendedSerializer[Payload] = avroKafkaSerializer

  override def deserializer(): ExtendedDeserializer[Payload] = avroKafkaDeserializer
}
