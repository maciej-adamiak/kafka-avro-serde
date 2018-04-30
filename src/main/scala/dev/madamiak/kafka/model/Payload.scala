package dev.madamiak.kafka.model

import spray.json._

/**
  * Transferred data enhanced with additional metadata
  *
  * @param strain  type of data, translated to schema name
  * @param version version of data schema
  * @param data    data convertible to json
  */
case class Payload(strain: String, version: Version, data: JsObject) {

  require(strain.nonEmpty, "payload strain should be nonempty to enable schema solving")
  require(version != null, "payload version should be nonempty to enable schema solving")
  require(data != null, "payload data should be nonempty")

}
