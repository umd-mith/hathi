import dispatch._, Defaults._
import java.io.{ File, PrintWriter }
import scala.concurrent.{ Await, Future }
import scala.concurrent.duration.Duration
import scala.util.{ Failure, Success }

object Downloader extends App {
  val collectionId = args(0)
  val outputFile = new File(args(1))
  val CountPattern = """.*All Items \((\d+)\).*""".r
  val IdPattern = """.*href="/cgi/pt\?id=([^"]+)".*""".r

  def createUrl(pageNumber: Int) =
    url("http://babel.hathitrust.org/cgi/mb") <<? Map(
      "c"  -> collectionId,
      "pn" -> pageNumber.toString,
      "a"  -> "listis",
      "sz" -> "100"
    )

  def getResponse(req: Req) = {
    println(s"Downloading ${ req.url }")
    Http(req OK as.String)
  }

  def getVolumeIds(body: String) = body.split("\\n").collect {
    case IdPattern(id) => id
  }

  def getPageCount(body: String) = body.split("\\n").collectFirst {
    case CountPattern(number) => math.ceil(number.toInt / 100.0).toInt 
  }.fold(Future.failed[Int](new Exception("Can't find page count.")))(
    Future.successful
  )

  val volumeIds = for {
    firstBody <- getResponse(createUrl(1))
    pageCount <- getPageCount(firstBody)
    firstIds  <- Future(getVolumeIds(firstBody))
    otherIds  <- Future.traverse(2 to pageCount)(pageNumber =>
      getResponse(createUrl(pageNumber)).map(getVolumeIds)
    )
  } yield firstIds ++ otherIds.flatten 

  volumeIds.onComplete {
    case Failure(error) => throw error
    case Success(results) =>
      val writer = new PrintWriter(outputFile)
      results.foreach(writer.println)
      writer.close()
  }

  Await.ready(volumeIds, Duration.Inf)
}

