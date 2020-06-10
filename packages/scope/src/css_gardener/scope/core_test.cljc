(ns css-gardener.scope.core-test
  (:require [clojure.test :refer [deftest testing is]]
            [css-gardener.scope.core :refer [scope-from-stylesheet]]
            #?(:cljs ["fs" :as fs])))

(defn- test-stylesheet-content
  [name]
  (let [relative-path (str "src/css_gardener/scope/test_stylesheets/" name)]
    #?(:clj (slurp relative-path)
       :cljs (fs/readFileSync relative-path "utf8"))))

(deftest t-scope-from-stylesheet
  (testing "Returns nil when no scope is defined in the stylesheet"
    (is (nil? (scope-from-stylesheet (test-stylesheet-content "no_scope.scss")))))
  (testing "Returns the scope when one exists"
    (is (= "test-stylesheet"
           (scope-from-stylesheet (test-stylesheet-content "scope.scss")))))
  (testing "Returns the scope if the 'metadata' map has more than one key
            value pair"
    (is (= "multiple-kv-pairs"
           (scope-from-stylesheet (test-stylesheet-content "multiple_kv_pairs.scss"))))))
