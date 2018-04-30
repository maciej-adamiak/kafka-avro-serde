package dev.madamiak.kafka.service

import dev.madamiak.kafka.model.Version
import org.apache.avro.Schema

import scala.concurrent.Future

trait SchemaRegistryService {

  /**
    * Resolve an avro schema
    *
    * @param strain  type of data, translated to schema name
    * @param version version of data schema
    * @return avro schema
    */
  def schema(strain: String, version: Version): Future[Schema]

}