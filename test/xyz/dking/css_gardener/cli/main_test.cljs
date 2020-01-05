(ns xyz.dking.css-gardener.cli.main-test
  (:require [xyz.dking.css-gardener.cli.main :as main]
            [clojure.test :refer [deftest testing is]]))

(deftest get-output-from-process-test
  (testing "Returns the data printed to stdout as a string"
    (is (= (main/get-output-from-process "." "echo" ["hello"])
           "hello"))))
