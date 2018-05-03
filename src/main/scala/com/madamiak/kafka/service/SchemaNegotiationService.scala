package com.madamiak.kafka.service

import com.madamiak.kafka.model.Version
import org.apache.avro.Schema

import scala.concurrent.Future

trait SchemaNegotiationService {

  /**
    * Resolve an avro schema
    *
    * @param strain  type of data, translated to schema name
    * @param version version of data schema
    * @return avro schema
    */
  def schema(strain: String, version: Version): Future[Schema]

}
