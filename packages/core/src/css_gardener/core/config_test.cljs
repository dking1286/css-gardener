(ns css-gardener.core.config-test
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [css-gardener.core.config :as sut]
            [css-gardener.core.utils.errors :as errors]
            [css-gardener.core.utils.testing :refer [testing
                                                     instrument-specs]]))

(use-fixtures :once instrument-specs)

(def ^:private config
  {:source-paths ["src"]
   :builds {}
   :rules
   {".css" {:transformers []}
    ".scss" {:dependency-resolver {:node-module "@css-gardener/sass-resolver"}
             :transformers [{:node-module "@css-gardener/sass-transformer"}]}}})

(deftest t-matching-rule
  (testing "Returns the rule in the configuration file matching the file"
    (is (= {:dependency-resolver {:node-module "@css-gardener/sass-resolver"}
            :transformers [{:node-module "@css-gardener/sass-transformer"}]}
           (sut/matching-rule config "/blah/blah.scss"))))
  (testing "Returns not-found when no rule matches the file"
    (is (errors/not-found? (sut/matching-rule config "/blah/blah.blah"))))
  (testing "Returns conflict when more than one rule matches the file"
    (let [config (assoc-in config [:rules "css"] {:transformers []})]
      (is (errors/conflict? (sut/matching-rule config "/blah/blah.scss"))))))