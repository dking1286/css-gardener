(ns xyz.dking.css-gardener.repl-test
  (:require [clojure.test :refer :all]
            [xyz.dking.css-gardener.repl :as repl]
            [xyz.dking.css-gardener.utils :as utils]))

(def ^:private ^:dynamic *repl-env* nil)

(defn- random-out-dir
  "Generates a random cache directory for the test repl.

  This ensures that the state of the test repl is independent of
  any other running repls."
  []
  (str ".css-gardener/repl-out-test-" (utils/uuid)))

(defn- with-repl-env
  "Test fixture that bootstraps a cljs repl for use in tests.

  The cljs repl is bound to *repl-env*."
  [run-tests]
  (let [repl-env (repl/new-repl-env (random-out-dir))]
    (repl/start-repl-env repl-env)
    (binding [*repl-env* repl-env]
      (run-tests))
    (repl/stop-repl-env repl-env)))

(use-fixtures :once with-repl-env)

(deftest ^:integration test-eval
  (testing "evaluates math expressions"
    (is (= 4 (repl/eval *repl-env* '(+ 2 2)))))
  (testing "can require and evaluate vars from cljs files"
    (repl/eval *repl-env*
               '(require (quote [xyz.dking.css-gardener.test-example.style-vars])))
    (is (= [:div {:background-color :green}]
           (repl/eval *repl-env*
                      'xyz.dking.css-gardener.test-example.style-vars/style)))))
