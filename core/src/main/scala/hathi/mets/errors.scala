package edu.umd.mith.hathi
package mets

case class MetsError(msg: String) extends Exception(msg)

case class UnexpectedMetsStructure(htid: Htid, msg: String) extends Exception(
  s"In $htid: $msg"
)
