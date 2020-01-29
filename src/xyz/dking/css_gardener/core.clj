(ns xyz.dking.css-gardener.core
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [xyz.dking.css-gardener.builder :as builder]
            [xyz.dking.css-gardener.builder.garden :as garden]
            [xyz.dking.css-gardener.builder.sass :as sass]
            [xyz.dking.css-gardener.config :as config]
            [xyz.dking.css-gardener.init :as init]
            [xyz.dking.css-gardener.utils :as utils]
            [xyz.dking.css-gardener.logging :as logging]
            [clojure.spec.alpha :as s]
            [hawk.core :as hawk]))

(def ^:private cached-files (atom {}))
(def ^:private done-watching? (promise))

(defn- get-builder
  "Gets an instance of the builder type specified in the config map."
  [config]
  (case (:type config)
    :garden (garden/new-builder config)
    :scss (sass/new-builder config)
    (throw (ex-info
            (str "Unknown :type property " (:type config) " found in config.")
            {:type :unknown-builder-type}))))

(defn- get-first-error
  [compiled-files]
  (first (filter :error compiled-files)))

(defn- compilation-error-message
  [{:keys [file error]}]
  (str "Error while compiling file " file ": " error))

(defn- success-message
  [output-file]
  (str "Wrote " output-file))

(defn- get-style-string
  [compiled-files]
  (->> compiled-files
       (map :result)
       (str/join "\n\n")))

(defn- write-output-file
  [output-file style-string]
  (let [output (io/file output-file)]
    (io/make-parents output)
    (spit output style-string)))

(s/fdef output-compiled-files
  :args (s/cat :compiled-files (s/coll-of ::builder/output-file)
               :output-files string?)
  :ret nil?)

(defn- output-compiled-files
  [compiled-files output-file]
  (if-some [error (get-first-error compiled-files)]
    (logging/error (compilation-error-message error))
    (let [style-string (get-style-string compiled-files)]
      (write-output-file output-file style-string)
      (logging/info (success-message output-file)))))

(defn- handle-file-change
  [builder output-file file]
  (let [filename (.getName file)
        abs-path (.getAbsolutePath file)]
    (when (builder/style-file? builder abs-path)
      (logging/info (str "Detected file changes: " filename))
      (let [file-info (config/file-details abs-path)
            compiled-file (builder/build-file builder file-info)]
        (swap! cached-files assoc (:file compiled-file) compiled-file)
        (let [compiled-files (vals @cached-files)]
          (output-compiled-files compiled-files output-file))))))

(defn init
  "Initializes a css-gardener project in the current directory."
  [config]
  (init/initialize-project config))

(defn build
  "Executes a single build of the user's stylesheet."
  [config]
  (let [b (get-builder config)
        full-config (config/augment-config config)
        output-file (:output-file config)]
    (builder/start b)
    (let [compiled-files (builder/build b full-config)]
      (output-compiled-files compiled-files output-file))
    (builder/stop b)))

(defn watch
  "Compiles the user's stylesheets on change."
  [config]
  (let [b (get-builder config)
        full-config (config/augment-config config)
        output-file (:output-file config)]
    (builder/start b)
    (let [compiled-files (builder/build b full-config)]
      (output-compiled-files compiled-files output-file)
      (reset! cached-files (utils/to-map :file compiled-files))
      (hawk/watch! [{:paths ["."]
                     :handler (fn [_ {:keys [file]}]
                                (handle-file-change b output-file file))}])
      @done-watching?)))

