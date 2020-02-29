(ns xyz.dking.css-gardener.utils.seq-test
  (:require [clojure.test :refer [deftest testing is]]
            [xyz.dking.css-gardener.utils.seq :refer [unique-by]]))

(deftest unique-by-test
  (testing "returns nil when nil is passed in."
    (is (= nil
           (unique-by identity nil))))
  (testing "returns an empty sequence if the passed in sequence is empty"
    (is (= '()
           (unique-by identity []))))
  (testing "returns the original sequence unmodified if the items are already unique"
    (is (= '("hello" "world")
           (unique-by identity ["hello" "world"]))))
  (testing "returns a sequence of unique values"
    (is (= '("hello" "world")
           (unique-by identity ["hello" "hello" "world"]))))
  (testing "uses the passed-in function to determine uniqueness"
    (is (= '({:name "hello"} {:name "world"})
           (unique-by :name [{:name "hello"}
                             {:name "hello"}
                             {:name "world"}])))))
