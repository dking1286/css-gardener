(ns xyz.dking.css-gardener.core
  (:require [clojure.spec.alpha :as s]
            [clojure.string :as str]
            [xyz.dking.css-gardener.builder :as builder]
            [xyz.dking.css-gardener.config :as config]
            [xyz.dking.css-gardener.init :as init]
            [xyz.dking.css-gardener.io :as gio]
            [xyz.dking.css-gardener.logging :as logging]
            [xyz.dking.css-gardener.utils :as utils]
            [xyz.dking.css-gardener.watcher :as watcher]))

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

(s/fdef output-compiled-files
  :args (s/cat :writer ::gio/writer
               :compiled-files (s/nilable (s/coll-of ::builder/output-file))
               :output-file string?)
  :ret nil?)

(defn- output-compiled-files
  [writer compiled-files output-file]
  (if-some [error (get-first-error compiled-files)]
    (logging/error (compilation-error-message error))
    (let [style-string (get-style-string compiled-files)]
      (gio/write-file writer output-file style-string)
      (logging/info (success-message output-file)))))

(defn- handle-file-change
  [builder reader writer cached-files output-file changed-file]
  (logging/info (str "Detected file changes: " changed-file))
  (let [file-info (file-details reader changed-file)
        compiled-file (builder/build-file builder file-info)]
    (swap! cached-files assoc (:file compiled-file) compiled-file)
    (let [compiled-files (vals @cached-files)]
      (output-compiled-files writer compiled-files output-file))))

(s/fdef init
  :args (s/cat :config ::config/config))

(defn init
  "Initializes a css-gardener project in the current directory."
  [config]
  (init/initialize-project config))

(s/fdef build
  :args (s/cat :builder ::builder/builder
               :reader ::gio/reader
               :writer ::gio/writer
               :config ::config/config))

(defn build
  "Executes a single build of the user's stylesheet."
  [builder reader writer config]
  (let [full-config (augment-config reader config)
        output-file (gio/get-absolute-path reader (:output-file config))
        input-files (:unique-input-files full-config)
        compiled-files (pmap #(builder/build-file builder %) input-files)]
    (output-compiled-files writer compiled-files output-file)))

(s/fdef watch
  :args (s/cat :builder ::builder/builder
               :watcher ::watcher/watcher
               :reader ::gio/reader
               :writer ::gio/writer
               :done? #(instance? clojure.lang.IDeref %)
               :config ::config/config))

(defn watch
  "Compiles the user's stylesheets on change."
  [builder watcher reader writer cached-files config]
  (let [full-config (augment-config reader config)
        output-file (gio/get-absolute-path reader (:output-file config))
        input-files (:unique-input-files full-config)
        compiled-files (pmap #(builder/build-file builder %) input-files)
        files-by-name (utils/to-map :file compiled-files)]
    (output-compiled-files writer compiled-files output-file)
    (reset! cached-files files-by-name)
    (watcher/watch
     watcher
     ["."]
     #(handle-file-change builder reader writer cached-files output-file %))))

