(ns xyz.dking.css-gardener.test-helpers
  (:require [xyz.dking.css-gardener.repl :as repl]
            [xyz.dking.css-gardener.utils :as utils]))

(def ^:dynamic *repl-env* nil)

(defn- random-out-dir
  "Generates a random cache directory for the test repl.

  This ensures that the state of the test repl is independent of
  any other running repls."
  []
  (str ".css-gardener/repl-out-test-" (utils/uuid)))

(defn with-repl-env
  "Test fixture that bootstraps a cljs repl for use in tests.

  The cljs repl is bound to *repl-env*."
  [run-tests]
  (let [repl-env (repl/new-repl-env (random-out-dir))]
    (repl/start-repl-env repl-env)
    (binding [*repl-env* repl-env]
      (run-tests))
    (repl/stop-repl-env repl-env)))
