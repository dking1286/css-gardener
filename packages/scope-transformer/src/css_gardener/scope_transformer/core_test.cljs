(ns css-gardener.scope-transformer.core-test
  (:require [clojure.test :refer [deftest testing is async]]
            [css-gardener.common.object :refer [object-merge]]
            [css-gardener.scope-transformer.core :refer [enter exit]]
            [fs]
            [goog.object :as object]
            [path]))

(defn- test-stylesheet-path
  [name]
  (path/resolve (str "src/css_gardener/scope_transformer/"
                     "test_stylesheets/"
                     name)))

(defn- test-stylesheet
  [name]
  (let [abs-path (test-stylesheet-path name)]
    #js {:absolutePath abs-path
         :content (fs/readFileSync abs-path "utf8")}))

(deftest t-enter-no-scope
  (testing "Yields the file unmodified if the stylesheet has no scope."
    (async done
      (enter (test-stylesheet "no_scope.scss")
             #js {}
             (fn [err result]
               (is (nil? err))
               (is (object/equals (test-stylesheet "no_scope.scss")
                                  result))
               (done))))))

(deftest t-enter-with-scope
  (testing "Yields the file with a 'scopeTransformerScope' property added."
    (async done
      (let [infile (test-stylesheet "scope.scss")
            expected (object-merge
                      infile #js {:scopeTransformerScope "test-stylesheet"})]
        (enter infile
               #js {}
               (fn [err result]
                 (is (nil? err))
                 (is (object/equals expected result))
                 (done)))))))

(deftest t-exit-no-scope
  (testing "Yields the file unmodified if no 'scopeTransformerScope' property
            exists"
    (async done
      (let [infile (test-stylesheet "no_scope.css")
            expected infile]
        (exit infile
              #js {}
              (fn [err result]
                (is (nil? err))
                (is (object/equals expected result))
                (done)))))))

(deftest t-exit-with-scope
  (testing "Yields the file with all classnames transformed if the scope exists"
    (async done
      (let [infile (object-merge
                    (test-stylesheet "scope.css")
                    #js {:scopeTransformerScope "test"})
            expected (object-merge
                      infile
                      #js {:content (object/get (test-stylesheet "scope_transformed.css") "content")})]
        (exit infile
              #js {}
              (fn [err result]
                (is (nil? err))
                (is (object/equals expected result))
                (done)))))))