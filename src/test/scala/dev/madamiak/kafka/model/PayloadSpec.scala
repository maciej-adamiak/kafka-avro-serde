package dev.madamiak.kafka.model

import org.scalatest.{Matchers, WordSpec}

class PayloadSpec extends WordSpec with Matchers {

  "payload" when {

    "handling JSON" should {
      import spray.json._

      "successfully write JSON" in {
        Payload("testStrain", Version(1, 2, 0), """{}""".parseJson.asJsObject).toJson.toString shouldEqual """{"strain":"testStrain","version":"1.2.0","data":{}}"""
      }

      "successfully read JSON" in {
        """{
          |   "strain":"testStrain",
          |   "version":"1.2.0",
          |   "data":{
          |   }
          |}
        """.stripMargin.parseJson.convertTo[Payload] shouldEqual Payload("testStrain", Version(1, 2, 0), """{}""".parseJson.asJsObject)
      }

      "be reflexive" in {
        val payload = Payload("testStrain", Version(1, 2, 0), """{}""".parseJson.asJsObject)
        payload.toJson.toString.parseJson.convertTo[Payload] shouldEqual payload
      }
    }

  }

}
