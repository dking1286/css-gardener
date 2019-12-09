(ns xyz.dking.css-gardener.repl-test
  (:require [clojure.test :refer :all]
            [xyz.dking.css-gardener.repl :as repl]
            [xyz.dking.css-gardener.utils :as utils]
            [xyz.dking.css-gardener.test-helpers :refer [*repl-env*
                                                         with-repl-env
                                                         start-repl-env]]))

(use-fixtures :once with-repl-env start-repl-env)

(deftest ^:integration test-eval
  (testing "evaluates math expressions"
    (is (= 4 (repl/eval *repl-env* '(+ 2 2)))))
  (testing "can require and evaluate vars from cljs files"
    (repl/eval *repl-env*
               '(require (quote [xyz.dking.css-gardener.test-example.style-vars])))
    (is (= [:div {:background-color :green}]
           (repl/eval *repl-env*
                      'xyz.dking.css-gardener.test-example.style-vars/style)))))
