(ns xyz.dking.css-gardener.io-test
  (:require [clojure.test :refer :all]
            [xyz.dking.css-gardener.io :as sut]
            [xyz.dking.css-gardener.test-helpers :refer :all]
            [clojure.java.io :as io]))

(deftest stub-reader-read-file-test
  (testing "returns nil when the file does not exist"
    (let [r (sut/new-stub-reader {"/hello/world.scss" "some styles"})]
      (is (nil? (sut/read-file r "/goodbye/world.scss")))))
  (testing "returns the file text when the file exists"
    (let [r (sut/new-stub-reader {"/hello/world.scss" "some styles"})]
      (is (= "some styles"
             (sut/read-file r "/hello/world.scss"))))))

(deftest file-reader-read-file-test
  (testing "returns nil when the file does not exist"
    (let [r (sut/new-file-reader)]
      (is (nil? (sut/read-file r "/idonot/exist")))))
  (testing "returns the file contents when the file exists"
    (with-fixture with-temp-file
      (spit *temp-file* "some styles")
      (let [r (sut/new-file-reader)]
        (is (= (sut/read-file r (.getAbsolutePath *temp-file*))
               "some styles"))))))

(deftest stub-writer-write-file-test
  (testing "writes the file to the files atom"
    (let [w (sut/new-stub-writer)]
      (sut/write-file w "/hello/world.txt" "some styles")
      (is (= (-> w :files deref (get "/hello/world.txt"))
             "some styles")))))

(deftest file-writer-write-file-test
  (testing "writes the text to the file system"
    (with-fixture with-temp-file
      (let [w (sut/new-file-writer)]
        (sut/write-file w (.getAbsolutePath *temp-file*) "some styles")
        (is (= (slurp *temp-file*) "some styles")))))
  (testing "creates parent directories if they do not exist"
    (with-fixture with-temp-file
      (let [w (sut/new-file-writer)
            abs-path (str (.getParent *temp-file*)
                          "/some_other_dir/"
                          (.getName *temp-file*))
            file (io/file abs-path)]
        (try
          (sut/write-file w abs-path "some styles")
          (is (= (slurp abs-path) "some styles"))
          (finally
            (if (.exists file)
              (.delete file))))))))
