(ns xyz.dking.css-gardener.builder.garden
  (:require [clojure.edn :as edn]
            [garden.core :as garden]
            [xyz.dking.css-gardener.analyzer :as analyzer]
            [xyz.dking.css-gardener.builder
             :as builder :refer [Builder get-builder]]
            [xyz.dking.css-gardener.repl :as repl]
            [xyz.dking.css-gardener.utils :as utils]))

(defn get-style
  "Gets the value of a namespace-qualified var in a cljs namespace."
  [repl-env style-var]
  (repl/eval repl-env `(~'require  (~'quote [~(namespace style-var)])))
  (repl/eval repl-env style-var))

(defn- get-all-styles
  "Gets a seq of the values of all the style vars in a seq of
  files."
  [repl-env files]
  (->> files
       analyzer/all-style-vars
       (map (partial get-style repl-env))))

(defrecord GardenBuilder [repl-env]
  Builder
  (start [this]
    (repl/start-repl-env repl-env))
  (stop [this]
    (repl/stop-repl-env repl-env))
  (build [this config]
    (let [files (utils/unique-files (:input-files config))
          styles (get-all-styles repl-env files)
          style-string (apply garden/css styles)]
      (spit (:output-file config) style-string)))
  (watch [this config]
    ;; Implement me
    ))

(defmethod get-builder :garden
  [_]
  (let [repl-env (repl/new-repl-env)]
    (->GardenBuilder repl-env)))

