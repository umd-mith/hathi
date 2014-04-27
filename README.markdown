HathiTrust utilities and tools
==============================

This project contains a variety of components that we use in projects at
[MITH](http://mith.umd.edu/) to interact with APIs at the
[HathiTrust Digital Library](http://www.hathitrust.org/) and to parse and
manipulate HathiTrust volume data and metadata.

Bibliographic API client
------------------------

The HathiTrust provides
[an API for bibliographic data](http://www.hathitrust.org/bib_api) that returns
record and volume level metadata, including
[MARC-XML](http://www.loc.gov/marc/marc.html) records. Our client allows you to
download the metadata for a batch of volume identifiers. See the
[`BibDownloader`](https://github.com/umd-mith/hathi/blob/master/core/src/main/scala/hathi/api/bib/cli/driver.scala)
class for a simple example that will generally provide all the functionality you
need to access this API.

The client sleeps for random periods between requests, and will retry failed
requests with exponential backoff.

Data API client
---------------

The HathiTrust also provides access to volume data through its
[Data API](http://www.hathitrust.org/data_api). You must have
[an access key](http://babel.hathitrust.org/cgi/kgs/request) to make requests
against this API, and many of its functions are not available to users who do
not have special authorization.

The API uses the
[one-legged OAuth](https://github.com/Mashape/mashape-oauth/blob/master/FLOWS.md#oauth-10a-one-legged)
authentication mechanism (note that the HathiTrust documentation describes this
as two-legged OAuth, but we follow what appears to be the more standard naming),
and all requests must be signed. We provide an implementation of the signing
mechanism that works with [Dispatch](http://dispatch.databinder.net/Dispatch.html),
a Scala HTTP client library.

See the [`DataDownloader`](https://github.com/umd-mith/hathi/blob/master/core/src/main/scala/hathi/api/data/cli/driver.scala)
class for an example of usage.

This client also by defaults provides retries for failures that are not related
to authorization.

Pairtrees
---------

The HathiTrust uses Pairtrees to store volume data and page-level metadata. We
provide a number of tools for working with Pairtree structures.

License
-------

All code is licensed under the [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0).
Several of the components in this repository were developed as part of a
partnership between MITH and the [Princeton Prosody Archive](https://digitalhumanities.princeton.edu/ppa/).
It is also being used in the [NEH](http://www.neh.gov/)-funded
[Active OCR](http://www.neh.gov/) project, and in MITH's new
[metadata correction project](http://mith.umd.edu/mith-awarded-hathitrust-research-center-grant/),
which is supported by a grant from the [HathiTrust Research Center](http://www.hathitrust.org/htrc)'s
[Workset Creation For Scholarly Analysis](http://worksets.htrc.illinois.edu/worksets/) project.
