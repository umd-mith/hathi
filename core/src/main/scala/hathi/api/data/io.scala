package edu.umd.mith.hathi
package api.data

import java.io.{ File, FileOutputStream, PrintWriter }
import scala.concurrent.Future

/** Additional client functionality for writing downloaded data to disk.
  */
trait DataClientIO { this: DataClient =>
  def saveBytes(bytes: Array[Byte], file: File): Future[Unit] = Future {
    val out = new FileOutputStream(file)
    out.write(bytes)
    out.close()
  }

  def saveString(text: String, file: File): Future[Unit] = Future {
    val out = new PrintWriter(file)
    out.println(text)
    out.close()
  }

  def saveAggregate(htid: Htid)(file: File): Future[Unit] =
    getAggregateBytes(htid).flatMap(bytes => saveBytes(bytes, file))

  def savePageImage(htid: Htid, seq: String)(file: File): Future[Unit] =
    getPageImageBytes(htid, seq).flatMap(bytes => saveBytes(bytes, file))

  def savePageOcr(htid: Htid, seq: String)(file: File): Future[Unit] =
    getPageOcr(htid, seq).flatMap(text => saveString(text, file))
}
