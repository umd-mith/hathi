package edu.umd.mith.util

import scalaz._, Scalaz._
import org.specs2.mutable._
import org.specs2.ScalaCheck

class PairtreeParserSpec extends Specification with ScalaCheck {
  "The default Pairtree parser configuration" should {
    "successfully round-trip any identifier to a path and back" ! prop { (s: String) =>
      import PairtreeParser.Default._

      toId(toPath(s)).fold(_ => false, _ === s)
    }
  }
}

