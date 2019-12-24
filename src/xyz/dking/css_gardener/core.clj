(ns xyz.dking.css-gardener.core
  (:require [xyz.dking.css-gardener.builder :as builder]))

(defn init
  "Initializes a css-gardener project in the current directory."
  [config]
  (throw (UnsupportedOperationException. "Init not implemented yet.")))

(defn build
  "Executes a single build of the user's stylesheet."
  [config]
  (let [b (builder/get-builder config)]
    (builder/start b)
    (builder/build b config)
    (builder/stop b)))

(defn watch
  [config]
  (throw (UnsupportedOperationException. "Watch not implemented yet.")))
