(ns css-gardener.core.utils.fs-test
  (:require [clojure.core.async :refer [<!]]
            [clojure.test :refer [testing is use-fixtures]]
            [clojure.string :as string]
            [css-gardener.core.utils.fs :as sut]
            [css-gardener.core.utils.errors :as errors]
            [css-gardener.core.utils.testing :refer [instrument-specs
                                                     deftest-async]]))

(use-fixtures :once instrument-specs)

(deftest-async exists-no-file-test
  (testing "returns a channel that yields false when the file does not exist"
    (is (= false (<! (sut/exists? "./blah/blah"))))))

(deftest-async exists-yes-file-test
  (testing "returns a channel that yields true when the file exists"
    (is (= true (<! (sut/exists? "./package.json"))))))

(deftest-async read-file-does-not-exist
  (testing "returns a channel that yields a not-found error when the file does not exist"
    (is (errors/not-found? (<! (sut/read-file "./blah/blah"))))))

(deftest-async read-file-exists
  (testing "returns a channel that yields the file contents as a string when reading is successful"
    (is (string/includes? (<! (sut/read-file "./package.json"))
                          "\"name\": \"@css-gardener/core\""))))