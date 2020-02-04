(ns xyz.dking.css-gardener.core-test
  (:require [clojure.string :as str]
            [clojure.test :refer :all]
            [xyz.dking.css-gardener.builder :as builder]
            [xyz.dking.css-gardener.builder.sass :as sass]
            [xyz.dking.css-gardener.core :refer [build watch]]
            [xyz.dking.css-gardener.io :as gio]
            [xyz.dking.css-gardener.test-helpers
             :refer
             [*temp-file* with-temp-file]]
            [xyz.dking.css-gardener.watcher :as watcher]))

(use-fixtures :each with-temp-file)

(deftest sass-builder-build-test
  (testing "logs an error when a stylesheet is invalid"
    (let [temp-file-name (.getAbsolutePath *temp-file*)
          config {:type :scss
                  :input-files ["test/xyz/dking/css_gardener/test_example/scss/invalid.scss"]
                  :output-file temp-file-name}
          builder (sass/new-builder config)
          reader (gio/new-file-reader)
          writer (gio/new-file-writer)
          output (with-out-str (build builder reader writer config))]
      (is (str/includes? output "Error while compiling file"))
      (is (str/includes? output "Invalid CSS"))))
  (testing "builds a css stylesheet from the passed-in list of files"
    (let [temp-file-name (.getAbsolutePath *temp-file*)
          config {:type :scss
                  :input-files ["test/xyz/dking/css_gardener/test_example/scss/_styles1.scss"]
                  :output-file temp-file-name}
          builder (sass/new-builder config)
          reader (gio/new-file-reader)
          writer (gio/new-file-writer)]
      (build builder reader writer config)
      (is (str/includes? (slurp temp-file-name)
                         "background-color: green"))))
  (testing "works with computed styles"
    (let [temp-file-name (.getAbsolutePath *temp-file*)
          config {:type :scss
                  :input-files ["test/xyz/dking/css_gardener/test_example/scss/styles3.scss"]
                  :output-file temp-file-name}
          builder (sass/new-builder config)
          reader (gio/new-file-reader)
          writer (gio/new-file-writer)]
      (build builder reader writer config)
      (is (str/includes? (slurp temp-file-name)
                         "background-color: red")))))

(deftest build-test
  (testing "logs an error when a stylesheet is invalid"
    (let [config {:type :scss
                  :input-files ["some/file"]
                  :output-file "some/output/file"}
          reader (gio/new-stub-reader {"/some/file" "Text of the file"}
                                      {"some/output/file" "/some/output/file"}
                                      {"some/file" ["/some/file"]})
          writer (gio/new-stub-writer)
          builder (builder/new-stub-builder {:output-prefix "There was a problem"
                                             :error? true})]
      (is (str/includes? (with-out-str (build builder reader writer config))
                         "There was a problem: Text of the file"))))
  (testing "writes the results from the builder to the output file"
    (let [config {:type :scss
                  :input-files ["some/file"]
                  :output-file "some/output/file"}
          reader (gio/new-stub-reader {"/some/file" "Text of the file"}
                                      {"some/output/file" "/some/output/file"}
                                      {"some/file" ["/some/file"]})
          writer (gio/new-stub-writer)
          builder (builder/new-stub-builder {:output-prefix "Input was"
                                             :error? false})]
      (build builder reader writer config)
      (is (str/includes? (gio/get-written-file writer "/some/output/file")
                         "Input was: Text of the file")))))

(deftest watch-test
  (testing "performs an initial build of the stylesheets"
    (let [config {:type :scss
                  :input-files ["some/file"]
                  :output-file "some/output/file"}
          watcher (watcher/new-stub-watcher)
          reader (gio/new-stub-reader {"/some/file" "Text of the file"}
                                      {"some/output/file" "/some/output/file"}
                                      {"some/file" ["/some/file"]})
          writer (gio/new-stub-writer)
          builder (builder/new-stub-builder {:output-prefix "Input was"
                                             :error? false})
          cached-files (atom {})]
      (watch builder watcher reader writer cached-files config)
      (is (str/includes? (gio/get-written-file writer "/some/output/file")
                         "Input was: Text of the file"))))
  (testing "rebuilds the output file when one of the input files changes"
    (let [config {:type :scss
                  :input-files ["some/file"]
                  :output-file "some/output/file"}
          watcher (watcher/new-stub-watcher)
          reader (gio/new-stub-reader {"/some/file" "Text of the file"}
                                      {"some/output/file" "/some/output/file"}
                                      {"some/file" ["/some/file"]})
          writer (gio/new-stub-writer)
          builder (builder/new-stub-builder {:output-prefix "Input was"
                                             :error? false})
          cached-files (atom {})]
      (watch builder watcher reader writer cached-files config)
      (is (not (str/includes? (gio/get-written-file writer "/some/output/file")
                              "Input was: Some other text")))
      (gio/update-file! reader "/some/file" "Some other text")
      (watcher/trigger-change-callback watcher "/some/file")
      (is (str/includes? (gio/get-written-file writer "/some/output/file")
                         "Input was: Some other text")))))
