HathiTrust utilities and tools
==============================

This project contains a variety of components that we use in projects at
[MITH](http://mith.umd.edu/) to interact with APIs at the
[HathiTrust Digital Library](http://www.hathitrust.org/) and to parse and
manipulate HathiTrust volume data and metadata.

Project organization
--------------------

The [`edu.umd.mith.util`](https://github.com/umd-mith/hathi/tree/master/util/src/main/scala/util)
package in the `util` project contains a number of generally useful tools for
developing API clients, working with Pairtree structures, etc. The
[`core`](https://github.com/umd-mith/hathi/tree/master/core) project includes
all HathiTrust-specific code.

Bibliographic API client
------------------------

The HathiTrust provides
[an API for bibliographic data](http://www.hathitrust.org/bib_api) that returns
record and volume level metadata, including
[MARC-XML](http://www.loc.gov/marc/marc.html) records. Our client allows you to
download the metadata for a batch of volume identifiers. See the
[`BibDownloader`](https://github.com/umd-mith/hathi/blob/master/core/src/main/scala/hathi/api/bib/cli/driver.scala)
class for a simple example that will generally provide all the functionality you
need when working with this API.

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
and all requests must be signed. We provide
[an implementation](https://github.com/umd-mith/hathi/blob/master/util/src/main/scala/util/oauth.scala)
of the signing mechanism that works with
[Dispatch](http://dispatch.databinder.net/Dispatch.html),
a Scala HTTP client library.

See the [`DataDownloader`](https://github.com/umd-mith/hathi/blob/master/core/src/main/scala/hathi/api/data/cli/driver.scala)
class for an example of usage.

This client also by defaults provides retries for failures that are not related
to authorization.

METS
----

The HathiTrust uses the [Metadata Encoding and Transmission Standard](https://github.com/umd-mith/hathi/blob/master/util/src/main/scala/util/oauth.scala)
to describe the files that make up a volume. The METS files are available both
through the Bibliographic API and in batch data sets. We provide a METS file
parser that is aware of the conventions of the HathiTrust's use of the standard.

Pairtrees
---------

The HathiTrust uses [Pairtrees](http://stackoverflow.com/a/23326372/334519) to
store volume data and page-level metadata. We provide a number of tools for
working with Pairtree structures, including
[an implementation](https://github.com/umd-mith/hathi/blob/master/util/src/main/scala/util/pairtree.scala)
of the path escaping and unescaping mechanism.

Usage examples
--------------

Suppose we've got a list of volume identifiers in a file—e.g. something like
this:

```
loc.ark:/13960/t05x2xb7f
nyp.33433081859120
dul1.ark:/13960/t5j970j9d
```

And we wanted to download the metadata records for these volumes from the
Bibliographic API. We could compile the project like this (note that this
requires you to have [Java](http://www.java.com/) 6 or newer installed; there
are no other dependencies):

``` bash
./sbt assembly
```

And then run the `BibDownloader` application:

``` bash
java -cp core/target/scala-2.10/hathi-core-assembly-0.0.0-SNAPSHOT.jar \
  edu.umd.mith.hathi.api.bib.cli.BibDownloader volume-list.txt output
```

This would create an `output` directory that would contain a file `failed.txt`
listing all volumes that could not be downloaded because of failed requests,
another file `missing.txt` listing all volumes that did not have results, and a
`results` directory. In this case `missing.txt` and `failed.txt` are empty, and
`results` contains three JSON files (one for each of the volumes in our list).

Now suppose we want to download all of the _data_ (as opposed to metadata) for
one of these volumes. We'd first create a `.hathitrustrc` file in our home
directory containing our consumer key and secret (the following is fake):

```
ec8b4b8e6b
105bf9f122496b2fce1c17117ef3
```

Now we run the `DataDownloader` application:

``` bash
java -cp core/target/scala-2.10/hathi-core-assembly-0.0.0-SNAPSHOT.jar \
  edu.umd.mith.hathi.api.data.cli.DataDownloader loc.ark:/13960/t05x2xb7 output
```

Now we also have `loc.ark+=13960=t05x2xb7f.mets.xml` and
`loc.ark+=13960=t05x2xb7f.zip` files in our `output` directory. If we unzip
the latter, we'll find all of the [JPEG-2000](https://en.wikipedia.org/wiki/JPEG_2000)
images, [DjVu](http://djvu.org/) XML OCR coordinate files, and text files for
this volume.

Note that while we provide a few simple command-line applications, this project
is designed primarily to be used as a library, and most of its functionality is
not exposed through applications.

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
