# zipple

[![Build Status](https://travis-ci.org/yetanalytics/zipple.svg?branch=master)](https://travis-ci.org/yetanalytics/zipple)

A tiny lib to make working with zips in Clojure a little less painful.

## Usage

``` clojure
(require '[zipple.core :refer :all])

(dotozip "tmp/dotozip.zip"
         ;; add a file
         (add "dotozip/file1.txt" (io/file "dev-resources/test/file1.txt"))
         ;; add a file by content
         (add "dotozip/foo.txt" "bar")
         ;; add a directory w/files
         (add "dotozip/test" (io/file "dev-resources/test"))
         ;; add an empty dir
         (add "dotozip/empty/")) ;; => a java.io.File
```

## License

Copyright © 2016 Yet Analytics Inc.
Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
