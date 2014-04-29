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

This client also by default provides retries for failures that are not related
to authorization.

METS
----

The HathiTrust uses the [Metadata Encoding and Transmission Standard](https://github.com/umd-mith/hathi/blob/master/util/src/main/scala/util/oauth.scala)
to describe the files that make up a volume. The METS files are available both
through the Bibliographic API and in batch data sets. We provide a METS file
parser that is aware of the conventions of the HathiTrust's use of the standard.

Pairtrees
---------

The HathiTrust uses [Pairtrees](https://wiki.ucop.edu/display/Curation/PairTree) to
store volume data and page-level metadata. We provide a number of tools for
working with Pairtree structures, including
[an implementation](https://github.com/umd-mith/hathi/blob/master/util/src/main/scala/util/pairtree.scala)
of the path escaping and unescaping mechanism.

Data structures
---------------

A [`Dataset`](https://github.com/umd-mith/hathi/blob/master/core/src/main/scala/hathi/dataset.scala)
represents a set of Pairtree structures (one for each library) containing METS
files and zipped text files in the form delivered by the HathiTrust over
Rsync or through some other bulk data transfer method (not the Data API). A
`Dataset` is constructed by pointing at the root directory containing the
library directories, and can be queried by volume identifier to get the pair of
files (METS and zipped text) for a volume, or to get a list of pages with
metadata directly.

A [`Collection`](https://github.com/umd-mith/hathi/blob/master/core/src/main/scala/hathi/collection.scala)
represents a `Dataset` together with a directory containing the JSON metadata
files for the volumes contained of the `Dataset`. A `Collection` can be queried
by volume identifier and will return both a list of pages and the volume
metadata.

Language and library API design
-------------------------------

These components are written in [Scala](http://www.scala-lang.org/), a language
that runs on the Java Virtual Machine, and they can be used from Java code. For
example, you could write the following to find a volume in a collection:

``` java
import edu.umd.mith.hathi.Collection;
import edu.umd.mith.hathi.Htid;
import edu.umd.mith.hathi.Volume;
import java.io.File;
import scala.util.Either;

public class JavaDemo {
  public static void main(String[] args) throws Throwable {
    Collection collection = new Collection(
      new File("collection/metadata/"),
      new File("collection/data")
    );

    String volumeId = "dul1.ark:/13960/t5j970j9d";

    Either<Throwable, Volume> volumeOrError =
      collection.volume(Htid.parse(volumeId)).toEither();

    Volume volume;

    if (volumeOrError.isLeft()) {
      throw volumeOrError.left().get();
    } else {
      volume = volumeOrError.right().get();
    }
  }
}
```

While this is not as concise as the equivalent Scala code would be, it's still
reasonably clear.

Many interfaces expose methods returning values of type `Throwable \/ A`, for
some type `A`. The `\/` here is a disjunction type from
[Scalaz](https://github.com/scalaz/scalaz), a library designed to support
functional programming in Scala. It is roughly equivalent to the Scala standard
library's [`Either`](http://www.scala-lang.org/api/2.10.4/index.html#scala.util.Either),
and can be converted to an `Either` through its `toEither` method.

Like `Either`, `\/` is often used to represent computations that may fail. If
we're validating that a string can be parsed into an integer, for example, it's
often not appropriate (or at least not ideal) to throw an exception on invalid
input, since that may not truly represent an "exceptional" situation. Instead
we can return a value of type `Either[Throwable, Int]` or `Throwable \/ Int`,
with the possible exception in the left side of the disjunction or the
successfully parsed value in the right side.

We use `\/` in many places here instead of `Either` because it allows _monadic_
composition. For example, we can write the following:

``` scala
def computeInt: Throwable \/ Int = ???
def computeString: Throwable \/ String = ???
def combine(i: Int, s: String): Foo = ???

def computeTwoThings: Throwable \/ Foo = for {
  i <- computeInt
  s <- computeString
} yield combine(i, s)
```

Now if either computation fails we'll end up with a failure (as desired). This
kind of composition is possible with `Either`, but is not so syntactically
convenient.

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
