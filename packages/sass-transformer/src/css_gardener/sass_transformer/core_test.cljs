(ns css-gardener.sass-transformer.core-test
  (:require [clojure.string :as string]
            [clojure.test :refer [deftest async testing is]]
            [css-gardener.sass-transformer.core :refer [enter exit]]
            [fs]
            [goog.object :as object]
            [path]))

(defn- test-stylesheet-path
  [name]
  (path/resolve (str "src/css_gardener/sass_transformer/"
                     "test_stylesheets/"
                     name)))

(defn- test-stylesheet
  [name]
  (let [abs-path (test-stylesheet-path name)]
    #js {:absolutePath abs-path
         :content (fs/readFileSync abs-path "utf8")}))

(deftest t-enter-no-absolute-path
  (testing "Yields an error if the input object is missing the
            absolutePath key"
    (async done
      (enter #js {:content ""}
             #js {}
             (fn [err result]
               (is (nil? result))
               (is (= "'absolutePath' key is missing on the input file"
                      (.-message err)))
               (done))))))

(deftest t-enter-no-content
  (testing "Yields an error if the input object is missing the content key"
    (async done
      (enter #js {:absolutePath "/some/style/file.scss"}
             #js {}
             (fn [err result]
               (is (nil? result))
               (is (= "'content' key is missing on the input file"
                      (.-message err)))
               (done))))))

(deftest t-enter-syntax-error
  (testing "yields an error if the sass file has a syntax error"
    (async done
      (enter (test-stylesheet "syntax_error.scss")
             #js {}
             (fn [err result]
               (is (nil? result))
               (is (instance? js/Error err))
               (done))))))

(deftest t-enter-no-dependencies
  (testing "yields the compiled file if there are no dependencies"
    (async done
      (let [infile (test-stylesheet "no_dependencies.scss")]
        (enter infile
               #js {}
               (fn [err result]
                 (is (nil? err))
                 (is (= (test-stylesheet-path "no_dependencies.scss")
                        (object/get result "absolutePath")))
                 (is (= (object/get infile "content")
                        (object/get result "sassTransformerOriginalContent")))
                 (is (string/includes? (object/get result "content")
                                       ".foo .bar {"))
                 (done)))))))

(deftest t-enter-with-dependencies
  (testing "yields the compiled file if there are @import dependencies"
    (async done
      (let [infile (test-stylesheet "import_dependency.scss")]
        (enter infile
               #js {}
               (fn [err result]
                 (is (nil? err))
                 (is (= (test-stylesheet-path "import_dependency.scss")
                        (object/get result "absolutePath")))
                 (is (= (object/get infile "content")
                        (object/get result "sassTransformerOriginalContent")))
                 (is (string/includes? (object/get result "content")
                                       "color: red;"))
                 (done)))))))

(deftest t-enter-with-use-dependencies
  (testing "yields the compiled file if there are @use dependencies"
    (async done
      (let [infile (test-stylesheet "use_dependency.scss")]
        (enter infile
               #js {}
               (fn [err result]
                 (is (nil? err))
                 (is (= (test-stylesheet-path "use_dependency.scss")
                        (object/get result "absolutePath")))
                 (is (= (object/get infile "content")
                        (object/get result "sassTransformerOriginalContent")))
                 (is (string/includes? (object/get result "content")
                                       "color: red;"))
                 (done)))))))

(deftest t-enter-with-indented-syntax
  (testing "compiles sass stylesheets with indented syntax"
    (async done
      (let [infile (test-stylesheet "use_dependency.sass")]
        (enter infile
               #js {:indentedSyntax true}
               (fn [err result]
                 (is (nil? err))
                 (is (= (test-stylesheet-path "use_dependency.sass")
                        (object/get result "absolutePath")))
                 (is (= (object/get infile "content")
                        (object/get result "sassTransformerOriginalContent")))
                 (is (string/includes? (object/get result "content")
                                       "color: red;"))
                 (done)))))))

(deftest t-exit-yields-file-unmodified
  (testing "exit yields the file unmodified"
    (async done
      (exit #js {:absolutePath "/hello/world"
                 :content "hello world"}
            #js {}
            (fn [err result]
              (is (nil? err))
              (is (object/equals #js {:absolutePath "/hello/world"
                                      :content "hello world"}
                                 result))
              (done))))))