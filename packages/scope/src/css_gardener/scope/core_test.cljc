(ns
  ^{:css-gardener/require ["test_stylesheets/scope.scss"]}
  css-gardener.scope.core-test
  #?(:clj (:require [clojure.test :refer [deftest testing is]]
                    [css-gardener.scope.core :refer [scope-from-stylesheet
                                                     scope-from-style-deps]]))
  #?(:clj (:import [java.io FileNotFoundException]))
  #?(:cljs (:require [clojure.test :refer [deftest testing is]]
                     [css-gardener.scope.core :refer [scope-from-stylesheet
                                                      infer-scope]]
                     [fs])))

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

#?(:clj
   (deftest t-scope-from-style-deps
     (testing "Returns the scope of the stylesheet if only one is passed"
       (is (= "test-stylesheet"
              (scope-from-style-deps "src/css_gardener/scope/core_test.cljc"
                                     ["test_stylesheets/scope.scss"]))))
     (testing "Returns the scope of the stylesheets if they all agree"
       (is (= "test-stylesheet"
              (scope-from-style-deps "src/css_gardener/scope/core_test.cljc"
                                     ["test_stylesheets/scope.scss"
                                      "test_stylesheets/scope_2.scss"]))))
     (testing "Throws an error if the file depends on a stylesheet that
               does not exist"
       (is (thrown?
            FileNotFoundException
            (scope-from-style-deps "src/css_gardener/scope/core_test.cljc"
                                   ["test_stylesheets/scope.scss"
                                    "test_stylesheets/i_do_not_exist.scss"]))))
     (testing "Throws an error if the stylesheets disagree about the scope"
       (is (thrown-with-msg?
            clojure.lang.ExceptionInfo #"Multiple scopes"
            (scope-from-style-deps "src/css_gardener/scope/core_test.cljc"
                                   ["test_stylesheets/scope.scss"
                                    "test_stylesheets/multiple_kv_pairs.scss"]))))))

#?(:cljs
   (deftest t-infer-scope
     (testing "Returns the scope of the stylesheets depended on by the
               current file"
       (is (= "test-stylesheet" (infer-scope))))))
