(ns xyz.dking.css-gardener.builder.garden-test
  (:require [clojure.string :as s]
            [clojure.test :refer :all]
            [xyz.dking.css-gardener.builder :as builder]
            [xyz.dking.css-gardener.builder.garden :as gbuilder]
            [xyz.dking.css-gardener.test-helpers :refer [*repl-env*
                                                         *temp-file*
                                                         with-repl-env
                                                         with-temp-file]]
            [xyz.dking.css-gardener.utils :as utils]))

(use-fixtures :once with-repl-env)
(use-fixtures :each with-temp-file)

(deftest ^:integration get-style-test
  (testing "reads style struct from the given var"
    (is (= (gbuilder/get-style
            *repl-env* 'xyz.dking.css-gardener.test-example.style-vars/style)
           [:div {:background-color :green}]))))

(deftest ^:integration build-test
  (testing "builds a css stylesheet from the passed in list of files"
    (let [gb (gbuilder/->GardenBuilder *repl-env*)
          temp-file-name (.getAbsolutePath *temp-file*)
          config {:input-files ["test/xyz/dking/css_gardener/test_example/style_vars.cljs"]
                  :output-file temp-file-name}]
      (builder/build gb config)
      (is (s/includes? (slurp temp-file-name)
                       "background-color: green")))))
