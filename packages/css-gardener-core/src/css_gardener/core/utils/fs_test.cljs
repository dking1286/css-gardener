(ns css-gardener.core.utils.fs-test
  (:require [clojure.core.async :refer [go <!]]
            [clojure.test :refer [deftest testing is async]]
            [css-gardener.core.utils.fs :as sut]
            [css-gardener.core.utils.errors :as errors]
            [clojure.string :as string]))

(deftest exists-no-file-test
  (testing "returns a channel that yields false when the file does not exist"
    (async done
           (go
             (is (= false (<! (sut/exists? "./blah/blah"))))
             (done)))))

(deftest exists-yes-file-test
  (testing "returns a channel that yields true when the file exists"
    (async done
           (go
             (is (= true (<! (sut/exists? "./package.json"))))
             (done)))))

(deftest read-file-does-not-exist
  (testing "returns a channel that yields a not-found error when the file does not exist"
    (async done
           (go
             (is (errors/not-found? (<! (sut/read-file "./blah/blah"))))
             (done)))))

(deftest read-file-throws-other-error
  (testing "returns a channel that yields an error when an unknown error happens while reading the file"))

(deftest read-file-exists
  (testing "returns a channel that yields the file contents as a string when reading is successful"
    (async done
           (go
             (is (string/includes? (<! (sut/read-file "./package.json"))
                                   "\"name\": \"@css-gardener/core\""))
             (done)))))