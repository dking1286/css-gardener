(ns xyz.dking.css-gardener.builder.garden-test
  (:require [clojure.string :as s]
            [clojure.test :refer :all]
            [xyz.dking.css-gardener.builder :as builder]
            [xyz.dking.css-gardener.builder.garden :as gbuilder]
            [xyz.dking.css-gardener.test-helpers :refer [*repl-env*
                                                         *temp-file*
                                                         with-repl-env
                                                         start-repl-env
                                                         with-temp-file]]
            [xyz.dking.css-gardener.utils :as utils]))

(def ^:private ^:dynamic *builder* nil)

(defn- with-builder
  [run-tests]
  (let [gb (gbuilder/->GardenBuilder (atom false) *repl-env*)]
    (builder/start gb)
    (binding [*builder* gb]
      (run-tests))
    (builder/stop gb)))

(use-fixtures :once with-repl-env)
(use-fixtures :each with-builder with-temp-file)

(deftest ^:integration build-test
  (testing "throws :not-started when build is called before start"
    (let [gb (gbuilder/->GardenBuilder (atom false) *repl-env*)]
      (is (thrown? clojure.lang.ExceptionInfo
                   (builder/build gb {})))))
  (testing "builds a css stylesheet from the passed in list of files"
    (let [temp-file-name (.getAbsolutePath *temp-file*)
          config {:input-files ["test/xyz/dking/css_gardener/test_example/style_vars.cljs"]
                  :output-file temp-file-name}]
      (builder/build *builder* config)
      (is (s/includes? (slurp temp-file-name)
                       "background-color: green"))))
  (testing "works with computed style vars"
    (let [temp-file-name (.getAbsolutePath *temp-file*)
          config {:input-files ["test/xyz/dking/css_gardener/test_example/computed_style_var.cljs"]
                  :output-file temp-file-name}]
      (builder/build *builder* config)
      (is (s/includes? (slurp temp-file-name)
                       "background-color: green"))
      (is (s/includes? (slurp temp-file-name)
                       "background-color: red")))))
