(ns xyz.dking.css-gardener.builder.sass-test
  (:require [clojure.test :refer :all]
            [xyz.dking.css-gardener.builder.sass :as sass]
            [xyz.dking.css-gardener.test-helpers
             :refer
             [*temp-file* with-temp-file]]
            [xyz.dking.css-gardener.builder :as builder]
            [clojure.string :as str]))

(def ^:private ^:dynamic *builder* nil)

(defn- with-builder
  [run-tests]
  (let [sb (sass/->ScssBuilder)]
    (builder/start sb)
    (binding [*builder* sb]
      (run-tests))
    (builder/stop sb)))

(use-fixtures :each with-builder with-temp-file)

(deftest build-test
  (testing "logs an error when a stylesheet is invalid"
    (let [temp-file-name (.getAbsolutePath *temp-file*)
          config {:input-files ["test/xyz/dking/css_gardener/test_example/scss/invalid.scss"]
                  :output-file temp-file-name}
          output (with-out-str (builder/build *builder* config))]
      (is (str/includes? output "Error while compiling scss file"))
      (is (str/includes? output "Invalid CSS"))))
  (testing "builds a css stylesheet from the passed-in list of files"
    (let [temp-file-name (.getAbsolutePath *temp-file*)
          config {:input-files ["test/xyz/dking/css_gardener/test_example/scss/_styles1.scss"]
                  :output-file temp-file-name}]
      (builder/build *builder* config)
      (is (str/includes? (slurp temp-file-name)
                         "background-color: green"))))
  (testing "works with computed styles"
    (let [temp-file-name (.getAbsolutePath *temp-file*)
          config {:input-files ["test/xyz/dking/css_gardener/test_example/scss/styles3.scss"]
                  :output-file temp-file-name}]
      (builder/build *builder* config)
      (is (str/includes? (slurp temp-file-name)
                         "background-color: red")))))
