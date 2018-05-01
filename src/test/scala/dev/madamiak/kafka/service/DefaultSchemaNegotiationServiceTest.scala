package dev.madamiak.kafka.service

import akka.actor.ActorSystem
import akka.http.scaladsl.model.{ HttpResponse, StatusCodes }
import akka.stream.ActorMaterializer
import dev.madamiak.kafka.model.Version
import org.scalamock.scalatest.MockFactory
import org.scalatest.{ Matchers, WordSpec }
import spray.json.DefaultJsonProtocol

import scala.concurrent.duration._
import scala.concurrent.{ Await, ExecutionContext, Future }

class DefaultSchemaNegotiationServiceTest extends WordSpec with Matchers with MockFactory with DefaultJsonProtocol {

  implicit val system: ActorSystem             = ActorSystem("test-kafka-avro-serde-system")
  implicit val executor: ExecutionContext      = system.dispatcher
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  private val responseEntity =
    """{
      |	"strain": "???",
      |	"version": "???",
      |	"schema": {
      |        "namespace": "generic",
      |        "type": "record",
      |        "doc": "test platform event",
      |        "name": "event",
      |        "fields":[
      |             {  "name": "amount", "type": "int"},
      |             {  "name": "message",  "type": "string"}
      |        ]   
      |    }
      |}""".stripMargin

  "schema registry" when {

    "fetching schema" should {

      "utilize cache" when {

        "calling single strain and version" in {
          val mock = mockFunction[String, Version, Future[HttpResponse]]

          val sut = new DefaultSchemaNegotiationService() {
            override def request(strain: String, version: Version): Future[HttpResponse] =
              mock(strain, version)
          }

          mock
            .expects(*, *)
            .returning(
              Future.apply(
                HttpResponse.apply(
                  entity = responseEntity
                )
              )
            )
            .once()

          Await.result(sut.schema("testStrain", Version("1.0.0-test")), Duration.Inf)
          Await.result(sut.schema("testStrain", Version("1.0.0-test")), Duration.Inf)
        }

        "calling single strain and multiple versions" in {
          val mock = mockFunction[String, Version, Future[HttpResponse]]

          val sut = new DefaultSchemaNegotiationService() {
            override def request(strain: String, version: Version): Future[HttpResponse] =
              mock(strain, version)
          }

          mock
            .expects(*, *)
            .returning(
              Future.apply(
                HttpResponse.apply(
                  entity = responseEntity
                )
              )
            )
            .twice()

          Await.result(sut.schema("testStrain", Version("1.0.0-test")), Duration.Inf)
          Await.result(sut.schema("testStrain", Version("1.5.0-test")), Duration.Inf)
          Await.result(sut.schema("testStrain", Version("1.0.0-test")), Duration.Inf)
        }
      }
    }

    "acquire valid result" in {
      val mock = mockFunction[String, Version, Future[HttpResponse]]

      val sut = new DefaultSchemaNegotiationService() {
        override def request(strain: String, version: Version): Future[HttpResponse] =
          mock(strain, version)
      }

      mock
        .expects("testStrain", Version("1.0.0-test"))
        .returning(
          Future.apply(
            HttpResponse.apply(
              entity = responseEntity
            )
          )
        )
        .once()

      Await
        .result(sut.schema("testStrain", Version("1.0.0-test")), Duration.Inf)
        .getFullName should equal("generic.event")
    }

    "fail when negotiation cannot be performed" in {
      val mock = mockFunction[String, Version, Future[HttpResponse]]

      val sut = new DefaultSchemaNegotiationService() {
        override def request(strain: String, version: Version): Future[HttpResponse] =
          mock(strain, version)
      }

      mock
        .expects("testStrain", Version("1.0.0-test"))
        .returning(
          Future.apply(
            HttpResponse.apply(
              status = StatusCodes.GatewayTimeout
            )
          )
        )
        .once()

      intercept[SchemaNegotiationException] {
        Await
          .result(sut.schema("testStrain", Version("1.0.0-test")), Duration.Inf)
          .getFullName should equal("generic.event")
      }
    }
  }

}
