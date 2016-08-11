(ns zipple.core-test
  (:require [clojure.test :refer :all]
            [zipple.core :refer :all]
            [clojure.java.io :as io])
  (:import [java.util.zip
            ZipEntry
            ZipException
            ZipOutputStream
            ZipInputStream
            ZipFile]
           [java.io File IOException]))

;; temp dir helpers

(defn empty-dir! [f]
  (doseq [sub-f (butlast (reverse (file-seq f)))]
    (.delete sub-f)))

(defn delete-dir! [f]
  (doseq [sub-f (reverse (file-seq f))]
    (.delete sub-f)))

(defn ensure-temp-dir! [dir-path]
  (let [f (io/file dir-path)]
    (if (.exists f)
      (empty-dir! f)
      (.mkdir f))))

(defn tear-down-temp-dir! [dir-path]
  (let [f (io/file dir-path)]
    (when (.exists f)
      (delete-dir! f))))

(defn clean-temp-fixture [temp-dir-path]
  (fn [f]
    (ensure-temp-dir! temp-dir-path)
    (f)
    (tear-down-temp-dir! temp-dir-path)))

(use-fixtures :each (clean-temp-fixture "tmp"))

(deftest add-entry!-test
  (with-open [zip (ZipOutputStream. (io/output-stream "tmp/add-entry.zip"))]
    (testing "add file"
      (add-entry! zip "foo/file1.txt" (io/file "dev-resources/test/file1.txt")))

    (testing "add empty dir"
      (add-entry! zip "foo/empty-dir/")))

  (let [zip-seq (-> (ZipFile. "tmp/add-entry.zip")
                    .entries
                    enumeration-seq)]
    (is (= 2 (count zip-seq)))
    (is (= #{"foo/file1.txt"
             "foo/empty-dir/"}
           (into #{}
                 (map #(.getName %))
                 zip-seq)))))

(deftest add-test
  (testing "Recursively add a directory"
    (with-open [zip (ZipOutputStream. (io/output-stream "tmp/add.zip"))]
      (add zip "foo/test" (io/file "dev-resources/test")))

    (let [dir-seq (file-seq (io/file "dev-resources/test"))
          zip-seq (-> (ZipFile. "tmp/add.zip")
                      .entries
                      enumeration-seq)]
      (is (= (count dir-seq)
             (count zip-seq))))))

(deftest dotozip-test
  (let [zip (dotozip "tmp/dotozip.zip"
                     ;; add a file
                     (add "dotozip/file1.txt" (io/file "dev-resources/test/file1.txt"))
                     ;; add a file by content
                     (add "dotozip/foo.txt" "bar")
                     ;; add a directory w/files
                     (add "dotozip/test" (io/file "dev-resources/test"))
                     ;; add an empty dir
                     (add "dotozip/empty/")
                     )]
    (is (= java.io.File (class zip)))
    (let [dir-seq (file-seq (io/file "dev-resources/test"))
          zip-seq (-> (ZipFile. zip)
                      .entries
                      enumeration-seq)]
      (is (= (+ (count dir-seq) 3) ;; the dir + the 2 files and empty dir
             (count zip-seq))))))
