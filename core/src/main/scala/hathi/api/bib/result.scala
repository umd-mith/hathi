package edu.umd.mith.hathi.api.bib

import argonaut._, Argonaut._
import edu.umd.mith.hathi.Htid
import scalaz.Monoid

/** Represents a single valid result.
  */
case class BibResult(records: Json, items: List[Json])

object BibResult {
  implicit def BibResultCodec: CodecJson[BibResult] =
    casecodec2(BibResult.apply, BibResult.unapply)("records", "items")
}

/** A convenience class for aggregating downloaded bibliographic data and for
  * tracking errors or missing files.
  */
case class BibResultSet(
  successful: Map[Htid, BibResult],
  missing: Set[Htid],
  failed: List[(List[Htid], Throwable)]
)

object BibResultSet {
  def success(successful: Map[Htid, BibResult], missing: Set[Htid]) = BibResultSet(
    successful,
    missing,
    Nil
  )

  def failure(batch: List[Htid], error: Throwable) = BibResultSet(
    Map.empty,
    Set.empty,
    (batch, error) :: Nil
  )

  implicit val BibResultMonoid: Monoid[BibResultSet] = Monoid.instance(
    (a, b) => BibResultSet(
      a.successful ++ b.successful,
      a.missing ++ b.missing,
      a.failed ++ b.failed
    ),
    BibResultSet(Map.empty, Set.empty, Nil)
  )
}
