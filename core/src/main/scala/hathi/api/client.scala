package edu.umd.mith.hathi.api

import edu.umd.mith.util.DispatchUtils
import org.jboss.netty.util.Timer
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

/** Default basic configuration and properties for any API client.
  */
trait BasicClient extends DispatchUtils {
  def backoffMaxRetries = 8
  def backoffDelay = 1.second

  implicit def timer: Timer
  implicit def executor: ExecutionContext

  def shutdown(): Unit = {
    http.shutdown()
    timer.stop()
  }
}
