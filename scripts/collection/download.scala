import dispatch._, Defaults._
import java.io.{ File, PrintWriter }

object Downloader extends App {
  val collectionId = args(0)
  val output = new PrintWriter(new File(args(1)))

  val CountPattern = """.*All Items \((\d+)\).*""".r
  val IdPattern = """.*href="/cgi/pt\?id=([^"]+)".*""".r
  val baseUrl = "http://babel.hathitrust.org/cgi/mb"

  def createUrl(pageNumber: Int) =
    url("http://babel.hathitrust.org/cgi/mb") <<? Map(
      "c"  -> collectionId,
      "pn" -> pageNumber.toString,
      "a"  -> "listis",
      "sz" -> "100"
    )

  def getResponse(req: Req) = {
    println(s"Downloading ${ req.url }")
    Http(req OK as.String).apply()
  }

  def getVolumeIds(body: String) = body.split("\\n").collect {
    case IdPattern(id) => id
  }

  val firstBody = getResponse(createUrl(1))
  val pageCount = firstBody.split("\\n").collectFirst {
    case CountPattern(number) => math.ceil(number.toInt / 100.0).toInt
  }.get

  getVolumeIds(firstBody).foreach(output.println)

  for {
    page <- 2 to pageCount
    id   <- getVolumeIds(getResponse(createUrl(page)))
  } output.println(id)

  output.close()
}

