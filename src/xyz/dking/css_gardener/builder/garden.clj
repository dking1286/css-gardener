(ns xyz.dking.css-gardener.builder.garden
  (:require [clojure.edn :as edn]
            [garden.core :as garden]
            [xyz.dking.css-gardener.analyzer :as analyzer]
            [xyz.dking.css-gardener.builder
             :as builder :refer [Builder get-builder]]
            [xyz.dking.css-gardener.repl :as repl]
            [xyz.dking.css-gardener.utils :as utils]))

(defn- all-files
  [{:keys [input-files] :as config}]
  (utils/unique-files input-files))

(defn- get-style
  [repl-env style-var]
  (repl/eval repl-env `(~'require  (~'quote [~(namespace style-var)])))
  (edn/read-string (repl/eval repl-env `(identity ~style-var))))

(defn- get-all-styles
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
      style-string))
  (watch [this config]
    ;; Implement me
    ))

(defn new-garden-builder
  []
  (let [repl-env (repl/new-repl-env)]
    (->GardenBuilder repl-env)))

(defmethod get-builder :garden
  [_]
  (new-garden-builder))

;; (def builder (new-garden-builder))
;; (def repl-env (:repl-env builder))

;; (builder/start builder)
;; (builder/build builder {:input-files ["test/xyz/dking/css_gardener/test_example/*"]})

;; (repl/eval repl-env '(require (quote [xyz.dking.css-gardener.test-example.style-vars])))
;; (repl/eval repl-env 'xyz.dking.css-gardener.test-example.style-vars/style)
;; (def foo 'bar)
;; `(~'require (~'quote [~foo]))
