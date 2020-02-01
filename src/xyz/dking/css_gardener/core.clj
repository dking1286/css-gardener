(ns xyz.dking.css-gardener.core
  (:require [clojure.java.io :as io]
            [clojure.spec.alpha :as s]
            [clojure.string :as str]
            [hawk.core :as hawk]
            [xyz.dking.css-gardener.builder :as builder]
            [xyz.dking.css-gardener.config :as config]
            [xyz.dking.css-gardener.init :as init]
            [xyz.dking.css-gardener.io :as gio]
            [xyz.dking.css-gardener.logging :as logging]
            [xyz.dking.css-gardener.utils :as utils]
            [xyz.dking.css-gardener.watcher :as watcher]))

(def ^:private cached-files (atom {}))

(s/fdef file-details
  :args (s/cat :reader ::gio/reader
               :file ::gio/absolute-path)
  :ret ::config/file-details)

(defn- file-details
  [reader file]
  {:file file
   :text (gio/read-file reader file)})

(s/fdef augment-config
  :args (s/cat :reader ::gio/reader
               :config ::config/config)
  :ret ::config/augmented-config)

(defn- augment-config
  [reader config]
  (let [unique-input-files (->> (gio/expand-globs reader (:input-files config))
                                (map #(file-details reader %)))]
    (assoc config :unique-input-files unique-input-files)))

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
  :args (s/cat :compiled-files (s/nilable (s/coll-of ::builder/output-file))
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
  [builder reader output-file changed-file]
  (when (builder/style-file? builder changed-file)
    (logging/info (str "Detected file changes: " changed-file))
    (let [file-info (file-details reader changed-file)
          compiled-file (builder/build-file builder file-info)]
      (swap! cached-files assoc (:file compiled-file) compiled-file)
      (let [compiled-files (vals @cached-files)]
        (output-compiled-files compiled-files output-file)))))

(defn init
  "Initializes a css-gardener project in the current directory."
  [config]
  (init/initialize-project config))

(defn build
  "Executes a single build of the user's stylesheet."
  [builder reader config]
  (let [full-config (augment-config reader config)
        output-file (gio/get-absolute-path reader (:output-file config))]
    (builder/start builder)
    (let [compiled-files (builder/build builder full-config)]
      (output-compiled-files compiled-files output-file))
    (builder/stop builder)))

(defn watch
  "Compiles the user's stylesheets on change."
  [builder watcher reader done? config]
  (let [full-config (augment-config reader config)
        output-file (:output-file config)]
    (builder/start builder)
    (let [compiled-files (builder/build builder full-config)]
      (output-compiled-files compiled-files output-file)
      (reset! cached-files (utils/to-map :file compiled-files))
      (watcher/watch watcher ["."] #(handle-file-change builder output-file %))
      @done?)))

