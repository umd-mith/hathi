HathiTrust collection downloader demo
-------------------------------------

Just run something like this:

``` bash
./sbt "run 1416797736 my-ids.txt"
```

Where the first argument after `run` is the collection identifier, 
and `my-ids.txt` will contain a list of the volume identifiers.

This was written in five minutes and is not guaranteed to be in any
way the least bit robust.

