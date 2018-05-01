package dev.madamiak.kafka.serializer

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import dev.madamiak.kafka.model.{ Payload, Version }
import dev.madamiak.kafka.service.SchemaRegistryService
import org.apache.avro.Schema
import org.apache.avro.Schema.Parser
import org.apache.kafka.common.header.internals.RecordHeaders
import org.scalamock.scalatest.MockFactory
import org.scalatest.{ Matchers, WordSpec }
import spray.json._
import tech.allegro.schema.json2avro.converter.AvroConversionException

import scala.collection.JavaConverters._
import scala.concurrent.{ ExecutionContext, Future }

class AvroKafkaSerdeSpec extends WordSpec with Matchers with MockFactory with DefaultJsonProtocol {

  implicit val schemaRegistryServiceStub: SchemaRegistryService = stub[SchemaRegistryService]

  implicit val system: ActorSystem             = ActorSystem("test-system")
  implicit val executor: ExecutionContext      = system.dispatcher
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  val sut = new AvroKafkaSerde

  val schemaV1: Schema = new Parser().parse("""
      |{
      |     "type": "record",
      |     "namespace": "com.example",
      |     "name": "FullName",
      |     "fields": [
      |       { "name": "first", "type": "string" },
      |       { "name": "last", "type": "int" }
      |     ]
      |}
    """.stripMargin)

  val schemaV2: Schema =
    new Parser().parse("""
      |{
      |     "type": "record",
      |     "namespace": "com.example",
      |     "name": "FullName",
      |     "fields": [
      |       { "name": "first", "type": "string" },
      |       { "name": "middle", "type": "string", "default": "valB" },
      |       { "name": "last", "type": "int" }
      |     ]
      |}
    """.stripMargin)

  "kafka avro serializer" when {

    sut.serializer().configure(Map("registry.negotiation.timeout" -> 300).asJava, false)

    "deserializing" should {

      "successfully process incoming bytes and produce a valid payload" in {
        schemaRegistryServiceStub.schema _ when ("testStrainA", Version("0.1.0")) returns Future
          .successful(schemaV1)

        val headers = new RecordHeaders()
          .add("strain", "testStrainA".getBytes("UTF-8"))
          .add("version", "0.1.0".getBytes("UTF-8"))

        val bytes = Array[Byte](8, 118, 97, 108, 65, 8)

        sut.deserializer().deserialize("", headers, bytes) shouldEqual Payload(
          "testStrainA",
          Version("0.1.0"),
          """
            | {
            |   "first": "valA",
            |   "last" : 4
            | }
          """.stripMargin.parseJson.asJsObject
        )
      }

      "successfully process incoming bytes serialized using a wider schema and produce a valid payload" in {
        schemaRegistryServiceStub.schema _ when ("testStrainA", Version("0.1.0")) returns Future
          .successful(schemaV2)

        val headers = new RecordHeaders()
          .add("strain", "testStrainA".getBytes("UTF-8"))
          .add("version", "0.1.0".getBytes("UTF-8"))

        val bytes = Array[Byte](8, 118, 97, 108, 65, 8, 118, 97, 108, 66, 8)

        sut.deserializer().deserialize("", headers, bytes) shouldEqual Payload(
          "testStrainA",
          Version("0.1.0"),
          """
            | {
            |   "first": "valA",
            |   "middle": "valB",
            |   "last" : 4
            | }
          """.stripMargin.parseJson.asJsObject
        )
      }
    }

    "serializing" should {

      "successfully process payload with applicable schema" in {
        schemaRegistryServiceStub.schema _ when ("testStrainA", Version("0.1.0")) returns Future
          .successful(schemaV1)

        val payload = Payload(
          "testStrainA",
          Version("0.1.0"),
          """
            | {
            |   "first": "valA",
            |   "last" : 4
            | }
          """.stripMargin.parseJson.asJsObject
        )

        sut.serializer().serialize("", new RecordHeaders(), payload) shouldEqual Array[Byte](8, 118, 97, 108, 65, 8)
      }

      "successfully process with wider schema " in {
        schemaRegistryServiceStub.schema _ when ("testStrainA", Version("0.1.0")) returns Future
          .successful(schemaV2)

        val payload = Payload(
          "testStrainA",
          Version("0.1.0"),
          """
            | {
            |   "first": "valA",
            |   "last" : 4
            | }
          """.stripMargin.parseJson.asJsObject
        )

        sut.serializer().serialize("", new RecordHeaders(), payload) shouldEqual Array[Byte](8, 118, 97, 108, 65, 8,
          118, 97, 108, 66, 8)
      }

      "fail during processing payload with non-applicable schema" in {
        schemaRegistryServiceStub.schema _ when ("testStrainA", Version("0.1.0")) returns Future
          .successful(schemaV2)

        val payload = Payload(
          "testStrainA",
          Version("0.1.0"),
          """
            | {
            |   "second": "valA",
            |   "last" : 4
            | }
          """.stripMargin.parseJson.asJsObject
        )

        intercept[AvroConversionException] {
          sut.serializer().serialize("", new RecordHeaders(), payload)
        }
      }
    }
  }

}
