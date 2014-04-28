package edu.umd.mith.hathi

case class Page(
  metadata: PageMetadata,
  contents: String
)

case class Volume(
  metadata: VolumeMetadata,
  pages: List[Page]
)
