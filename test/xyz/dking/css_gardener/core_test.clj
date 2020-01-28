(ns xyz.dking.css-gardener.core-test
  (:require [clojure.test :refer :all]
            [xyz.dking.css-gardener.core :refer [build]]
            [xyz.dking.css-gardener.test-helpers
             :refer
             [*temp-file* with-temp-file]]
            [clojure.string :as str]))

(use-fixtures :each with-temp-file)

(deftest sass-builder-build-test
  (testing "logs an error when a stylesheet is invalid"
    (let [temp-file-name (.getAbsolutePath *temp-file*)
          config {:type :scss
                  :input-files ["test/xyz/dking/css_gardener/test_example/scss/invalid.scss"]
                  :output-file temp-file-name}
          output (with-out-str (build config))]
      (is (str/includes? output "Error while compiling file"))
      (is (str/includes? output "Invalid CSS"))))
  (testing "builds a css stylesheet from the passed-in list of files"
    (let [temp-file-name (.getAbsolutePath *temp-file*)
          config {:type :scss
                  :input-files ["test/xyz/dking/css_gardener/test_example/scss/_styles1.scss"]
                  :output-file temp-file-name}]
      (build config)
      (is (str/includes? (slurp temp-file-name)
                         "background-color: green"))))
  (testing "works with computed styles"
    (let [temp-file-name (.getAbsolutePath *temp-file*)
          config {:type :scss
                  :input-files ["test/xyz/dking/css_gardener/test_example/scss/styles3.scss"]
                  :output-file temp-file-name}]
      (build config)
      (is (str/includes? (slurp temp-file-name)
                         "background-color: red")))))

