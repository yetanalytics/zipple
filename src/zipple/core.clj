(ns zipple.core
  (:require [clojure.java.io :as io])
  (:import [java.util.zip
            ZipEntry
            ZipException
            ZipOutputStream]))

(defn zip-directory
  [dir-path zip-path & {:keys [add-files]
                        :or {add-files []}}]
  (with-open [zip (ZipOutputStream. (io/output-stream zip-path))]
    (doseq [f (file-seq (io/file dir-path)) :when (.isFile f)]
      (.putNextEntry zip (ZipEntry. (.getPath f)))
      (io/copy f zip)
      (.closeEntry zip))

    ;; add extra files
    (doseq [[path content] add-files]
      (.putNextEntry zip (ZipEntry. path))
      (io/copy content zip)
      (.closeEntry zip))))
