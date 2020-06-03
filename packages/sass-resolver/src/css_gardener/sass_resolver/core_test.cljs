(ns css-gardener.sass-resolver.core-test
  (:require [clojure.test :refer [deftest testing is async]]
            [css-gardener.sass-resolver.core :refer [main]]
            [fs]
            [goog.array :as array]
            [goog.object :as gobj]
            [path]))

(defn- test-stylesheet-path
  [name]
  (path/resolve (str "src/css_gardener/sass_resolver/"
                     "test_stylesheets/"
                     name)))

(defn- test-stylesheet
  [name]
  (let [abs-path (test-stylesheet-path name)]
    #js {:absolutePath abs-path
         :content (fs/readFileSync abs-path "utf8")}))

(deftest t-main-no-absolute-path
  (testing "Yields an error if the input object is missing the
            absolutePath key"
    (async done
      (main #js {:content ""}
            #js {}
            (fn [err result]
              (is (nil? result))
              (is (= "'absolutePath' key is missing on the input file"
                     (.-message err)))
              (done))))))

(deftest t-main-no-content
  (testing "Yields an error if the input object is missing the content key"
    (async done
      (main #js {:absolutePath "/some/style/file.scss"}
            #js {}
            (fn [err result]
              (is (nil? result))
              (is (= "'content' key is missing on the input file"
                     (.-message err)))
              (done))))))

(deftest t-main-no-deps
  (testing "Yields an empty array if the sass file has no dependencies"
    (async done
      (main (test-stylesheet "no_deps.scss")
            #js {}
            (fn [error result]
              (is (nil? error))
              (is (array/isEmpty result))
              (done))))))

(deftest t-main-import-dependencies
  (testing "Detects dependencies with the @import directive"
    (async done
      (main (test-stylesheet "import.scss")
            #js {}
            (fn [error result]
              (is (nil? error))
              (is (array/equals #js [(test-stylesheet-path "dependency.scss")]
                                result))
              (done))))))

(deftest t-main-use-dependencies
  (testing "Detects dependencies with the @use directive"
    (async done
      (main (test-stylesheet "use.scss")
            #js {}
            (fn [error result]
              (is (nil? error))
              (is (array/equals #js [(test-stylesheet-path "dependency.scss")]
                                result))
              (done))))))

(deftest t-main-multiple-dependencies
  (testing "Detects multiple dependencies"
    (async done
      (main (test-stylesheet "multi_use.scss")
            #js {}
            (fn [error result]
              (is (nil? error))
              (is (array/equals #js [(test-stylesheet-path "dependency.scss")
                                     (test-stylesheet-path "dependency_2.scss")]
                                (.sort result)))
              (done))))))

(deftest t-main-absolute-path-dependencies
  (testing "Handles absolute path dependencies in stylesheets"
    (async done
      (main (test-stylesheet "absolute_path.scss")
            #js {}
            (fn [error result]
              (is (nil? error))
              (is (array/equals #js ["/some/dependency.scss"]
                                result))
              (done))))))

(deftest t-main-indented-syntax
  (testing "appends a .sass extension if the 'indentedSyntax' option is true"
    (async done
      (main (test-stylesheet "use.scss")
            #js {:indentedSyntax true}
            (fn [error result]
              (is (nil? error))
              (is (array/equals #js [(test-stylesheet-path "dependency.sass")]
                                result))
              (done))))))

(deftest t-main-error
  (testing "yields an error if an error is thrown"
    (async done
      (with-redefs [gobj/get #(throw (js/Error. "Boom"))]
        (main (test-stylesheet "use.scss")
              #js {}
              (fn [error result]
                (is (nil? result))
                (is (instance? js/Error error))
                (is (= "Boom" (.-message error)))
                (done)))))))