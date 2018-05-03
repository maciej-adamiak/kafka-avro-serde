package com.madamiak.kafka

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json.{ DefaultJsonProtocol, JsString, JsValue, JsonFormat, RootJsonFormat }

package object model extends SprayJsonSupport with DefaultJsonProtocol {

  implicit val versionFormat: JsonFormat[Version] = new JsonFormat[Version] {
    override def write(obj: Version): JsValue = JsString(obj.toString)

    override def read(json: JsValue): Version =
      Version(json.convertTo[String])
  }

  implicit val payloadFormat: RootJsonFormat[Payload] = jsonFormat3(Payload)

}
