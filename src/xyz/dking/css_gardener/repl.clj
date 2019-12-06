(ns xyz.dking.css-gardener.repl
  (:refer-clojure :exclude [eval])
  (:require [clojure.edn :as edn]
            [cljs.analyzer :as analyzer]
            [cljs.analyzer.api :as analyzer-api]
            [cljs.env :as env]
            [cljs.repl :as repl]
            [cljs.repl.rhino :as rhino]
            [me.raynes.fs :as fs]))

(defn new-repl-env
  "Creates a new cljs repl env.

  If repl-out-dir is provided, the repl will cache its
  output files in that directory, relative to the project root.
  Otherwise, defaults to '.css-gardener/repl-out'."
  ([] (new-repl-env ".css-gardener/repl-out"))
  ([repl-out-dir]
   (-> (rhino/repl-env)
       (assoc :working-dir repl-out-dir
              :compiler-state (analyzer-api/empty-state)))))

(defn- clean-repl-out-dir
  "Clears the cache directory of a cljs repl env.

  Used to ensure that new repl envs start with a fresh state."
  [repl-env]
  (fs/delete-dir (:working-dir repl-env)))

(defn start-repl-env
  "Bootstraps a cljs repl based on the provided repl-env."
  [repl-env]
  (clean-repl-out-dir repl-env)
  (binding [env/*compiler* (analyzer-api/empty-state)]
    (repl/-setup repl-env {:output-dir (:working-dir repl-env)})))

(defn stop-repl-env
  "Shuts down the cljs repl corresponding to the passed repl-env."
  [repl-env]
  (repl/-tear-down repl-env)
  (clean-repl-out-dir repl-env))

(defn eval
  "Evaluates a form in the passed cljs repl."
  [repl-env form]
  (binding [env/*compiler* (:compiler-state repl-env)
            analyzer/*cljs-ns* 'cljs.user]
    (edn/read-string (repl/eval-cljs repl-env (analyzer-api/empty-env) form))))
