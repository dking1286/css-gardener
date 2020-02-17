(ns xyz.dking.css-gardener.core
  (:require [clojure.spec.alpha :as s]
            [clojure.string :as str]
            [xyz.dking.css-gardener.builder :as builder]
            [xyz.dking.css-gardener.config :as config]
            [xyz.dking.css-gardener.init :as init]
            [xyz.dking.css-gardener.io :as gio]
            [xyz.dking.css-gardener.logging :as logging]
            [xyz.dking.css-gardener.utils :as utils]
            [xyz.dking.css-gardener.watcher :as watcher]
            [clojure.tools.namespace.dependency :as dependency])
  (:import [clojure.lang IDeref]))

(def lock (Object.))

(defn- file-details
  [reader file]
  {:file file
   :text (gio/read-file reader file)})

(s/fdef file-details
  :args (s/cat :reader ::gio/reader
               :file ::gio/absolute-path)
  :ret ::config/file-details)

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

(defn- output-compiled-files
  [writer compiled-files output-file]
  (if-some [error (get-first-error compiled-files)]
    (logging/error (compilation-error-message error))
    (let [style-string (get-style-string compiled-files)]
      (gio/write-file writer output-file style-string)
      (logging/info (success-message output-file)))))

(s/fdef output-compiled-files
  :args (s/cat :writer ::gio/writer
               :compiled-files (s/nilable (s/coll-of ::builder/output-file))
               :output-file string?)
  :ret nil?)

(defn- get-files-to-recompile
  [dependency-graph input-file-paths changed-file]
  (if (contains? input-file-paths changed-file)
    ;; The changed file is one of the input files, it is the only one that
    ;; needs to be recompiled
    #{changed-file}
    ;; The changed file is not an input file, we need to recompile
    ;; any input files that depend on it.
    (dependency/transitive-dependents dependency-graph changed-file)))

(s/fdef get-files-to-recompile
  :args (s/cat :dependency-graph ::builder/dependency-graph
               :input-file-paths (s/coll-of ::gio/absolute-path :kind set?)
               :changed-file ::gio/absolute-path)
  :ret (s/coll-of ::gio/absolute-path :kind set?))

(defn- recompile
  [reader builder files-to-recompile file]
  (if-not (contains? files-to-recompile (:file file))
    file
    (let [new-file-details (file-details reader (:file file))]
      (builder/build-file builder new-file-details))))

(s/fdef recompile
  :args (s/cat :reader ::gio/reader
               :builder ::builder/builder
               :files-to-recompile (s/coll-of ::gio/absolute-path :kind set?)
               :file ::builder/output-file)
  ;; TODO: This return spec is not complete, doesn't contain the result
  :ret (s/and ::config/file-details))

(defn- handle-file-change
  [builder reader writer cache input-file-globs output-file changed-file]
  ;; Lock to ensure only one change is processed at a time
  (locking lock
    (let [input-file-paths
          (set (gio/expand-globs reader input-file-globs))
          
          cached-files
          (:files-by-name @cache)
          
          dependency-graph
          (:dependency-graph @cache)
          
          files-to-recompile
          (get-files-to-recompile dependency-graph input-file-paths
                                  changed-file)
          
          compiled-files
          (pmap #(recompile reader builder files-to-recompile %)
                (vals cached-files))]
      ;; Update the cached files and dependency graph
      (swap! cache assoc
             :files-by-name (utils/to-map :file compiled-files)
             :dependency-graph (builder/get-dependency-graph builder
                                                             compiled-files))
      (output-compiled-files writer compiled-files output-file))))

(defn init
  "Initializes a css-gardener project in the current directory."
  [config]
  (init/initialize-project config))

(s/fdef init
  :args (s/cat :config ::config/config))

(defn build
  "Executes a single build of the user's stylesheet."
  [builder reader writer config]
  (let [input-file-globs (:input-files config)
        input-files (->> (gio/expand-globs reader input-file-globs)
                         (map #(file-details reader %)))
        output-file (gio/get-absolute-path reader (:output-file config))
        compiled-files (pmap #(builder/build-file builder %) input-files)]
    (output-compiled-files writer compiled-files output-file)))

(s/fdef build
  :args (s/cat :builder ::builder/builder
               :reader ::gio/reader
               :writer ::gio/writer
               :config ::config/config))

(defn watch
  "Compiles the user's stylesheets on change."
  [builder watcher reader writer cache config]
  (let [input-file-globs (:input-files config)
        input-files (->> (gio/expand-globs reader input-file-globs)
                         (map #(file-details reader %)))
        output-file (gio/get-absolute-path reader (:output-file config))
        compiled-files (pmap #(builder/build-file builder %) input-files)
        files-by-name (utils/to-map :file compiled-files)
        dependency-graph (builder/get-dependency-graph builder compiled-files)]
    (output-compiled-files writer compiled-files output-file)
    (swap! cache assoc
           :files-by-name files-by-name
           :dependency-graph dependency-graph)
    (watcher/watch
     watcher
     ["."]
     #(handle-file-change builder reader writer cache
                          input-file-globs output-file %))))

(s/fdef watch
  :args (s/cat :builder ::builder/builder
               :watcher ::watcher/watcher
               :reader ::gio/reader
               :writer ::gio/writer
               :cache #(instance? IDeref %)
               :config ::config/config))
