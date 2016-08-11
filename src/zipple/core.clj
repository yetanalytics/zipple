(ns zipple.core
  (:require [clojure.java.io :as io])
  (:import [java.util.zip
            ZipEntry
            ZipException
            ZipOutputStream
            ZipInputStream
            ZipFile]
           [java.io File IOException]))



(defn add-entry!
  "Add an entry to a zip output stream.
   Entries without content are assumed to be directories!"
  ([^ZipOutputStream zip
    ^String path]
   (.putNextEntry zip (ZipEntry. path))
   (.closeEntry zip))
  ([^ZipOutputStream zip
    ^String path
    content]
   (.putNextEntry zip (ZipEntry. path))
   (io/copy content zip)
   (.closeEntry zip)))

(defmulti add
  "Add an entry to a zip output stream, special handling based on content."
  (fn
    ([_ _]
     ::directory)
    ([_ _ content]
      (class content))))

(defmethod add java.io.File
  [^ZipOutputStream zip
   ^String zip-path
   ^File file-or-dir]
  (if (.isDirectory file-or-dir)
    (let [abs-dir-path (.getAbsolutePath file-or-dir)]
      (doseq [f (file-seq file-or-dir)
              :let [abs-file-path (.getAbsolutePath f)
                    path (str zip-path
                              (subs abs-file-path
                                    (count abs-dir-path)))]]
        (if (.isFile f)
          (add-entry! zip
                      path
                      f)
          (add-entry! zip
                      (str path "/")))))
    (add-entry! zip zip-path file-or-dir)))

(defmethod add ::directory
  [^ZipOutputStream zip
   ^String zip-path]
  (add-entry! zip zip-path))

(defmethod add :default
  [^ZipOutputStream zip
   ^String zip-path
   content]
  (add-entry! zip zip-path content))

(defn file?
  "Is it a file?"
  [maybe-file]
  (instance? java.io.File maybe-file))

(defn zip-output-stream
  "creates a new zip output stream.
   f can be any valid arg to clojure.java.io/output-stream"
  [f]
  (ZipOutputStream. (io/output-stream f)))

(defmacro dotozip
  "Like doto, but takes a zip file/path, creates a stream,
   applies the body fns, closes the stream and returns the file."
  [zip & body]
  `(let [zip-file# ~(if (file? zip)
                      zip
                      `(clojure.java.io/file ~zip))]
     (with-open [zip# (zip-output-stream zip-file#)]
       (doto zip#
         ~@body))
     zip-file#))

(defmacro let-zip
  [[sym f] & body]
  `(let [zip-file# ~(if (file? f)
                      f
                      `(clojure.java.io/file ~f))]
     (with-open [~sym (zip-output-stream zip-file#)]
       ~@body)
     zip-file#))

(defmacro compose
  "Creates a zip at the given path.
   All subsequent args are expected to be pairs of path strings and
   files/directories/strings. A nil file is assumed to be an empty dir."
  [zip
   & contents]
  {:pre [(even? (count contents))]}
  `(dotozip ~zip
          ~@(for [[path content] (partition 2 contents)]
              (if content
                `(add ~path ~content)
                `(add ~path)))))

(defn zip-entry-seq
  "Given a fille object or path, returns a seq of entries in a zip file."
  [f]
  (enumeration-seq (.entries (ZipFile. f))))
