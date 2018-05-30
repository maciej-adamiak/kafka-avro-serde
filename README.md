[![Build Status](https://travis-ci.org/maciej-adamiak/kafka-avro-serde.svg?branch=master)](https://travis-ci.org/maciej-adamiak/kafka-avro-serde)
[![codecov](https://codecov.io/gh/maciej-adamiak/kafka-avro-serde/branch/master/graph/badge.svg)](https://codecov.io/gh/maciej-adamiak/kafka-avro-serde)
[![CodeFactor](https://www.codefactor.io/repository/github/maciej-adamiak/kafka-avro-serde/badge)](https://www.codefactor.io/repository/github/maciej-adamiak/kafka-avro-serde)

# Kafka Avro serializer/deserializer

Serializer and deserializer implementation that utilizes [Avro](http://avro.apache.org/) format to store and read data from Kafka. Uses an external, configurable schema registry.

## Configuration

### Reference configuration

| property                         | default    | description                                        |
| ---                              | ---        | ---                                                |
| registry.host                    | localhost  | schema registry service host                       |
| registry.port                    | 8000       | schema registry service port                       |
| registry.path                    | enrollment | registry API path used to get schemas              |
| registry.schema.key              | schema     | name of the JSON object containing the Avro schema |
| registry.schema.cache.expiration | 60m        | time of caching a schema instance                  |
| registry.schema.cache.size       | 10000      | maximum number of cached schemas                   |

### Serializer


## Usage

//TODO add an example

## Schema registry service

Project contains a default schema registry HTTP service implementation. To be able to negotiate the schema the API has to meet following criteria:

- have a {registry.path}/{strain}/{version} GET method that produces a JSON object
- the result JSON object has to contain a single schema object available using {registry.schema.key} key

Request:
```bash
curl -X GET \
  http://localhost:8000/enrollment/test-event/2.0.1 \
  -H 'Content-Type: application/json' \
  -d '{
	"id": 123,
	"strain": "test-event",
	"version": "2.0.1",
	"payload": {
		"amount": 17,
		"message": "Karcia to łobuz"
	}
}'
``` 

Response:
```json
{
    "strain": "test-event",
    "version": "2.0.1",
    "schema": {
        "name": "event",
        "doc": "test platform event",
        "fields": [
            {
                "name": "amount",
                "type": "int"
            },
            {
                "name": "message",
                "type": "string"
            }
        ],
        "type": "record",
        "namespace": "generic"
    }
}
```

There is a possibility to define a custom schema registry service
