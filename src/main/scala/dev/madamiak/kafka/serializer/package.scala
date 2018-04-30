package dev.madamiak.kafka

package object serializer {

  implicit class RichString(val array: Array[Byte]) extends AnyVal {

    /**
      * @return a string from a byte array using utf-8 encoding
      */
    def utf8() = new String(array, "UTF-8")
  }

  implicit class RichByteArray(val string: String) extends AnyVal {

    /**
      * @return a byte array from a string using utf-8 encoding
      */
    def utf8(): Array[Byte] = string.getBytes("UTF-8")
  }

}
