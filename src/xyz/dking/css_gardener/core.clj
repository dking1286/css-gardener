(ns xyz.dking.css-gardener.core
  (:require [xyz.dking.css-gardener.builder :as builder]
            [xyz.dking.css-gardener.builder.garden :as garden]))

(defn- get-builder
  "Gets an instance of the builder type specified in the config map."
  [config]
  (case (:type config)
    :garden (garden/new-builder config)
    (throw (ex-info
            (str "Unknown :type property " (:type config) " found in config.")
            {:type :unknown-builder-type}))))

(defn init
  "Initializes a css-gardener project in the current directory."
  [config]
  (throw (UnsupportedOperationException. "Init not implemented yet.")))

(defn build
  "Executes a single build of the user's stylesheet."
  [config]
  (let [b (get-builder config)]
    (builder/start b)
    (builder/build b config)
    (builder/stop b)))

(defn watch
  [config]
  (throw (UnsupportedOperationException. "Watch not implemented yet.")))
