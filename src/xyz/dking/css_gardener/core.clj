(ns xyz.dking.css-gardener.core
  (:require [xyz.dking.css-gardener.builder :as builder]
            [xyz.dking.css-gardener.builder.garden :as garden]
            [xyz.dking.css-gardener.builder.sass :as sass]
            [xyz.dking.css-gardener.init :as init]
            [xyz.dking.css-gardener.utils :as utils]))

(defn- get-builder
  "Gets an instance of the builder type specified in the config map."
  [config]
  (case (:type config)
    :garden (garden/new-builder config)
    :scss (sass/new-builder config)
    (throw (ex-info
            (str "Unknown :type property " (:type config) " found in config.")
            {:type :unknown-builder-type}))))

(defn file-details
  [file]
  {:file file
   :text (slurp file)})

(defn augment-config
  [config]
  (let [unique-input-files (->> (utils/unique-files (:input-files config))
                                (map file-details))]
    (assoc config :unique-input-files unique-input-files)))

(defn init
  "Initializes a css-gardener project in the current directory."
  [config]
  (init/initialize-project config))

(defn build
  "Executes a single build of the user's stylesheet."
  [config]
  (let [b (get-builder config)
        full-config (augment-config config)]
    (builder/start b)
    (builder/build b full-config)
    (builder/stop b)
    (System/exit 0)))

(defn watch
  [config]
  (throw (UnsupportedOperationException. "Watch not implemented yet.")))
