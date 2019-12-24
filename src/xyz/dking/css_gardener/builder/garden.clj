(ns xyz.dking.css-gardener.builder.garden
  (:require [garden.core :as garden]
            [xyz.dking.css-gardener.analyzer :as analyzer]
            [xyz.dking.css-gardener.builder :refer [Builder get-builder]]
            [xyz.dking.css-gardener.errors :as errors]
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

(defrecord GardenBuilder [started? repl-env]
  Builder
  (start [this]
    (repl/start-repl-env repl-env)
    (reset! started? true))

  (stop [this]
    (repl/stop-repl-env repl-env)
    (reset! started? false))

  (build [this config]
    (if-not @started?
      (throw (errors/not-started (str "start must be called before build"))))
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
    (->GardenBuilder (atom false) repl-env)))

