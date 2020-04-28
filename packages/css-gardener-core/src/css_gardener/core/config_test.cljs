(ns css-gardener.core.config-test
  (:require [clojure.test :refer [deftest testing is]]
            [css-gardener.core.config :as sut]
            [css-gardener.core.utils.errors :as errors]))

(def config
  {:source-paths ["src"]
   :builds {}
   :rules
   {#"\.css$" {:transformers []}
    #"\.scss$" {:dependency-resolver {:node-module "@css-gardener/sass-resolver"}
                :transformers [{:node-module "@css-gardener/sass-transformer"}]}}})

(deftest t-matching-rule
  (testing "Returns the rule in the configuration file matching the file"
    (let [file {:absolute-path "/blah/blah.scss"
                :content ""}]
      (is (= {:dependency-resolver {:node-module "@css-gardener/sass-resolver"}
              :transformers [{:node-module "@css-gardener/sass-transformer"}]}
             (sut/matching-rule config file)))))
  (testing "Returns not-found when no rule matches the file"
    (let [file {:absolute-path "/blah/blah.blah"
                :content ""}]
      (is (errors/not-found? (sut/matching-rule config file)))))
  (testing "Returns conflict when more than one rule matches the file"
    (let [config (assoc-in config [:rules #"css$"] {:transformers []})
          file {:absolute-path "/blah/blah.scss"
                :content ""}]
      (is (errors/conflict? (sut/matching-rule config file))))))