(ns xyz.dking.css-gardener.repl
  (:refer-clojure :exclude [eval])
  (:require [cljs.analyzer :as analyzer]
            [cljs.analyzer.api :as analyzer-api]
            [cljs.env :as env]
            [cljs.repl :as repl]
            [cljs.repl.rhino :as rhino]
            [me.raynes.fs :as fs]))

(def repl-out-dir ".css-gardener/repl-out")

(def analyzer-env (analyzer-api/empty-env))
(def compiler-state (analyzer-api/empty-state))

(defn new-repl-env
  []
  (-> (rhino/repl-env)
      (assoc :working-dir repl-out-dir)))

(defn- clean-repl-out-dir
  []
  (fs/delete-dir repl-out-dir))

(defn start-repl-env
  [repl-env]
  (clean-repl-out-dir)
  (binding [env/*compiler* (analyzer-api/empty-state)]
    (repl/-setup repl-env {:output-dir repl-out-dir})))

(defn stop-repl-env
  [repl-env]
  (repl/-tear-down repl-env))

(defn eval
  [repl-env form]
  (binding [env/*compiler* compiler-state
            analyzer/*cljs-ns* 'cljs.user]
    (repl/eval-cljs repl-env analyzer-env form)))
