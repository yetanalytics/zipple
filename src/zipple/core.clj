(ns zipple.core
  (:require [clojure.java.io :as io])
  (:import [java.util.zip
            ZipEntry
            ZipException
            ZipOutputStream]
           [java.io File]))

(defn add-entry! [^ZipOutputStream zip
                 ^String path
                 content]
  (.putNextEntry zip (ZipEntry. path))
  (io/copy content zip)
  (.closeEntry zip))

(defn zip<-file
  "Add a file to a zip, with an optional new path."
  ([^ZipOutputStream zip
    ^File            f]
   (zip<-file zip f (.getPath f)))
  ([^ZipOutputStream zip
    ^File            f
    ^String          path]
   {:pre [(not (.isDirectory f))]}
   (add-entry! zip path f)))

(defn zip<-directory
  "Recursively add dir and files to zip output.
   Optional base-path will be used as prefix."
  ([^ZipOutputStream zip
    ^File            dir]
   (zip<-directory zip dir ""))
  ([^ZipOutputStream zip
    ^File            dir
    ^String          base-path]
   {:pre [(.isDirectory dir)]}
   (doseq [f (file-seq dir) :when (.isFile f)]
     (add-entry! zip
                 (str base-path (.getPath f))
                 f))))

(defn zip<-string
  "Add an entry to a zip from a string"
  [^ZipOutputStream zip
   ^String          s
   ^String          path]
  (add-entry! zip path s))

(defn file? [maybe-file]
  (instance? java.io.File maybe-file))

(defn compose-zip
  "Creates a zip at the given path.
   All subsequent args are expected to be pairs of path/base-path strings and
   files/directories/strings.

   ex: (compose-zip \"foo.zip\" ;; zip path
             ;; base path + directory
             \"foo/\" (io/file \"dev-resources/test\")
             ;; full path + File
             \"foo/some-file.txt\" (io/file \"some-file.txt\")
             ;; full path + string
             \"foo/bar.txt\" \"baz\")"
  [zip-path
   & contents]
  {:pre [(even? (count contents))]}
  (with-open [zip (ZipOutputStream. (io/output-stream zip-path))]
    (doseq [[path content] (partition 2 contents)]
      (if (file? content)
        (if (.isDirectory content)
          (zip<-directory zip content path)
          (zip<-file zip content path))
        (zip<-string zip content path)))))

(defn zip-directory
  "Recursively zip a directory"
  [dir-path zip-path]
  (with-open [zip (ZipOutputStream. (io/output-stream zip-path))]
    (zip<-directory zip (io/file dir-path))))
