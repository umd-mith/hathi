package edu.umd.mith.hathi.mets

trait MetsModel {
  val knownLabels = Set(
    "MULTIWORK_BOUNDARY",
    "COVER",
    "FRONT_COVER",
    "TITLE",
    "COPYRIGHT",
    "TABLE_OF_CONTENTS",
    "FIRST_CONTENT_CHAPTER_START",
    "CHAPTER_START",
    "REFERENCES",
    "INDEX",
    "BACK_COVER",
    "BLANK",
    "UNTYPICAL_PAGE",
    "IMPLICIT_PAGE_NUMBER",
    "IMAGE_ON_PAGE",
    // The following are currently ignored.
    "FRONT_COVER_FLAP",
    "BACK_COVER_FLAP",
    "INSIDE_FRONT_COVER",
    "PREFACE",
    "CHECKOUT_PAGE",
    "RIGHT,COVER",
    "FRONT_COVER_IMAGE_CORRECTION",
    "PAGE_TURNBACK",
    "RIGHT",
    "LEFT",
    "MISSING_PAGE",
    "MISSING",
    "FOLDOUT",
    "UNS",
    "ERRATA",
    "TP",
    "IND"
  )
}
