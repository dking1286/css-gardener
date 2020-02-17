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
            [xyz.dking.css-gardener.watcher :as watcher])
  (:import [java.io File]))

(use-fixtures :each with-temp-file)

(deftest sass-builder-build-test
  (testing "logs an error when a stylesheet is invalid"
    (let [temp-file-name (.getAbsolutePath ^File *temp-file*)
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
    (let [temp-file-name (.getAbsolutePath ^File *temp-file*)
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
    (let [temp-file-name (.getAbsolutePath ^File *temp-file*)
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
                         "Input was: Some other text"))))
  (testing "does not recompile files that have not changed"
    (let [config {:type :scss
                  :input-files ["some/file" "some/other/file"]
                  :output-file "some/output/file"}
          watcher (watcher/new-stub-watcher)
          reader (gio/new-stub-reader {"/some/file" "Text of the file"
                                       "/some/other/file" "Text of the other file"}
                                      {"some/output/file" "/some/output/file"}
                                      {"some/file" ["/some/file"]
                                       "some/other/file" ["/some/other/file"]})
          writer (gio/new-stub-writer)
          builder (builder/new-stub-builder {:output-prefix "Input was"
                                             :error? false})
          cached-files (atom {})]
      (watch builder watcher reader writer cached-files config)
      (gio/update-file! reader "/some/file" "Some other text")
      (watcher/trigger-change-callback watcher "/some/file")
      (is (not (str/includes? (gio/get-written-file writer "/some/output/file")
                              "Input was: Text of the file")))
      (is (str/includes? (gio/get-written-file writer "/some/output/file")
                         "Input was: Some other text"))
      (is (str/includes? (gio/get-written-file writer "/some/output/file")
                         "Input was: Text of the other file"))))
  (testing "does not compile a new file that does not match the input file globs"
    (let [config {:type :scss
                  :input-files ["some/file" "some/other/file"]
                  :output-file "some/output/file"}
          watcher (watcher/new-stub-watcher)
          reader (gio/new-stub-reader {"/some/file" "Text of the file"
                                       "/some/other/file" "Text of the other file"}
                                      {"some/output/file" "/some/output/file"}
                                      {"some/file" ["/some/file"]
                                       "some/other/file" ["/some/other/file"]})
          writer (gio/new-stub-writer)
          builder (builder/new-stub-builder {:output-prefix "Input was"
                                             :error? false})
          cached-files (atom {})]
      (watch builder watcher reader writer cached-files config)
      (gio/update-file! reader "/some/third/file" "Some third text")
      (watcher/trigger-change-callback watcher "/some/third/file")
      (is (not (str/includes? (gio/get-written-file writer "/some/output/file")
                              "Input was: Some third text")))))
  (testing "compiles files that depend on the changed file"
    (let [config {:type :scss
                  :input-files ["some/file" "some/other/file"]
                  :output-file "some/output/file"}
          watcher (watcher/new-stub-watcher)
          reader (gio/new-stub-reader {"/some/file" "Text of the file"
                                       "/some/other/file" "Text of the other file"}
                                      {"some/output/file" "/some/output/file"}
                                      {"some/file" ["/some/file"]
                                       "some/other/file" ["/some/other/file"]})
          writer (gio/new-stub-writer)
          builder (builder/new-stub-builder {:output-prefix "Input was"
                                             :error? false
                                             :dependencies {"/some/file" ["/some/third/file"]}})
          cached-files (atom {})]
      (watch builder watcher reader writer cached-files config)
      ;; Change the text of /some/file to simulate a change in its dependencies
      (gio/update-file! reader "/some/file" "Changed text")
      ;; Trigger the change callback for a dependency of /some/file
      (watcher/trigger-change-callback watcher "/some/third/file")
      ;; /some/file should have been recompiled
      (is (str/includes? (gio/get-written-file writer "/some/output/file")
                         "Input was: Changed text"))))
  (testing "caches each change"
    (let [config {:type :scss
                  :input-files ["some/file" "some/other/file"]
                  :output-file "some/output/file"}
          watcher (watcher/new-stub-watcher)
          reader (gio/new-stub-reader {"/some/file" "Text of the file"
                                       "/some/other/file" "Text of the other file"}
                                      {"some/output/file" "/some/output/file"}
                                      {"some/file" ["/some/file"]
                                       "some/other/file" ["/some/other/file"]})
          writer (gio/new-stub-writer)
          builder (builder/new-stub-builder {:output-prefix "Input was"
                                             :error? false
                                             :dependencies {"/some/file" ["/some/third/file"]}})
          cache (atom {})]
      (watch builder watcher reader writer cache config)
      ;; Change /some/file
      (gio/update-file! reader "/some/file" "Changed text")
      (watcher/trigger-change-callback watcher "/some/file")
      ;; Change /some/other/file
      (gio/update-file! reader "/some/other/file" "Other changed text")
      (watcher/trigger-change-callback watcher "/some/other/file")
      ;; Changes to /some/other/file should be present in the output
      (is (str/includes? (gio/get-written-file writer "/some/output/file")
                         "Input was: Other changed text"))
      ;; Changes to /some/file should persist in the output
      (is (str/includes? (gio/get-written-file writer "/some/output/file")
                         "Input was: Changed text"))))
  (testing "picks up changes to the dependencies"
    (let [config {:type :scss
                  :input-files ["some/file" "some/other/file"]
                  :output-file "some/output/file"}
          watcher (watcher/new-stub-watcher)
          reader (gio/new-stub-reader {"/some/file" "Text of the file"
                                       "/some/other/file" "Text of the other file"}
                                      {"some/output/file" "/some/output/file"}
                                      {"some/file" ["/some/file"]
                                       "some/other/file" ["/some/other/file"]})
          writer (gio/new-stub-writer)
          builder (builder/new-stub-builder {:output-prefix "Input was"
                                             :error? false
                                             :dependencies {"/some/file" ["/some/third/file"]}})
          cache (atom {})]
      (watch builder watcher reader writer cache config)
      ;; Add another dependency of /some/file
      (builder/update-dependencies! builder "/some/file" ["/some/third/file"
                                                          "/some/fourth/file"])
      (watcher/trigger-change-callback watcher "/some/file")
      ;; /some/file should be recompiled when the new dependency changes
      (gio/update-file! reader "/some/file" "Changed text")
      (watcher/trigger-change-callback watcher "/some/fourth/file")
      (is (str/includes? (gio/get-written-file writer "/some/output/file")
                         "Input was: Changed text")))))
