(ns xyz.dking.css-gardener.test-helpers
  (:require [clojure.java.io :as io]
            [xyz.dking.css-gardener.repl :as repl]
            [xyz.dking.css-gardener.utils :as utils])
  (:import [java.io File]))

(def ^:dynamic *repl-env* nil)
(def ^:dynamic *temp-file* nil)

(defmacro with-fixture
  [fixture & body]
  `(~fixture (fn [] ~@body)))

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
    (binding [*repl-env* repl-env]
      (run-tests))))

(defn start-repl-env
  "Test fixture that starts and stops the repl bound to
  *repl-env*."
  [run-tests]
  (repl/start-repl-env *repl-env*)
  (run-tests)
  (repl/stop-repl-env *repl-env*))

(defn with-temp-file
  "Test fixture that creates a temp file with a random name for testing.

  The temp file is bound to *temp-file*."
  [run-tests]
  (let [file (File/createTempFile
              (str "css-gardener-test-" (utils/uuid))
              ".txt")]
    (.deleteOnExit file)
    (try
      (binding [*temp-file* file]
        (run-tests))
      (finally
        (.delete file)))))

