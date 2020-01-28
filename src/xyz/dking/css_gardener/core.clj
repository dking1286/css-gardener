(ns xyz.dking.css-gardener.core
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [xyz.dking.css-gardener.builder :as builder]
            [xyz.dking.css-gardener.builder.garden :as garden]
            [xyz.dking.css-gardener.builder.sass :as sass]
            [xyz.dking.css-gardener.config :as config]
            [xyz.dking.css-gardener.init :as init]
            [xyz.dking.css-gardener.utils :as utils]
            [xyz.dking.css-gardener.logging :as logging]))

(defn- get-builder
  "Gets an instance of the builder type specified in the config map."
  [config]
  (case (:type config)
    :garden (garden/new-builder config)
    :scss (sass/new-builder config)
    (throw (ex-info
            (str "Unknown :type property " (:type config) " found in config.")
            {:type :unknown-builder-type}))))

(defn get-first-error
  [compiled-files]
  (first (filter :error compiled-files)))

(defn compilation-error-message
  [{:keys [file error]}]
  (str "Error while compiling file " file ": " error))

(defn get-style-string
  [compiled-files]
  (->> compiled-files
       (map :result)
       (str/join "\n\n")))

(defn write-output-file
  [output-file style-string]
  (let [output (io/file output-file)]
    (io/make-parents output)
    (spit output style-string)))

(defn init
  "Initializes a css-gardener project in the current directory."
  [config]
  (init/initialize-project config))

(defn build
  "Executes a single build of the user's stylesheet."
  [config]
  (let [b (get-builder config)
        full-config (config/augment-config config)]
    (builder/start b)
    (let [compiled-files (builder/build b full-config)]
      (if-some [error (get-first-error compiled-files)]
        (logging/error (compilation-error-message error))
        (let [style-string (get-style-string compiled-files)]
          (write-output-file (:output-file config) style-string))))
    (builder/stop b)))

(defn watch
  [config]
  (throw (UnsupportedOperationException. "Watch not implemented yet.")))
