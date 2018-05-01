package dev.madamiak.kafka

import java.time.{Duration => JDuration}

import scala.concurrent.duration.{FiniteDuration, Duration => SDuration}

package object service {

  implicit def toScalaDuration(jDuration: JDuration): FiniteDuration =
    SDuration.fromNanos(jDuration.toNanos)

}
