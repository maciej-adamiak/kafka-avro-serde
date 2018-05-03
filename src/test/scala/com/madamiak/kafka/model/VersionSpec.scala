package com.madamiak.kafka.model

import org.scalatest.{ Matchers, WordSpec }

import scala.util.Random

class VersionSpec extends WordSpec with Matchers {

  "versioning" when {

    "comparing" should {

      "produce a proper ordering" in {

        val v1       = Version(0, 0, 1)
        val v2       = Version(0, 1, 1)
        val v3       = Version(1, 0, 0)
        val v4       = Version(1, 0, 0)
        val v5       = Version(1, 0, 0, "a_test")
        val v6       = Version(1, 0, 0, "b_test")
        val v7       = Version(13, 0, 0)
        val v8       = Version(50, 50, 50)
        val versions = List(v1, v2, v3, v4, v5, v6, v7, v8)

        Random.shuffle(versions).sorted should contain inOrderElementsOf versions
      }

    }

    "creating its string representation" should {

      "generate standard semantic versioning" in {
        Version(1, 2, 0).toString shouldEqual "1.2.0"
      }

      "generate standard semantic versioning when pre release is null" in {
        Version(1, 2, 0, null).toString shouldEqual "1.2.0"
      }

      "generate extended semantic versioning" in {
        Version(1, 2, 0, "test").toString shouldEqual "1.2.0-test"
      }

    }

    "creating its instance using string representation" should {

      "generate standard semantic versioning" in {
        val version = Version("1.2.0")
        version.major shouldEqual 1
        version.minor shouldEqual 2
        version.patch shouldEqual 0
        version.pre shouldEqual None
      }

      "generate extended semantic versioning" in {
        val version = Version("1.2.0-test")
        version.major shouldEqual 1
        version.minor shouldEqual 2
        version.patch shouldEqual 0
        version.pre shouldEqual Some("test")
      }

      "fail in generating standard semantic versioning" in {
        intercept[IllegalArgumentException] {
          Version("1.2")
        }
      }

    }

    "handling JSON" should {

      import spray.json._

      "successfully write JSON" in {
        Version(1, 2, 0).toJson.toString shouldEqual """"1.2.0""""
      }

      "successfully read JSON" in {
        """"1.2.0"""".stripMargin.parseJson.convertTo[Version] shouldEqual Version(1, 2, 0)
      }

      "be reflexive" in {
        val version = Version(1, 2, 0)
        version.toJson.toString.parseJson.convertTo[Version] shouldEqual version
      }
    }

  }

}
