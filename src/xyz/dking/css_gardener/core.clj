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
            [clojure.tools.namespace.dependency :as dependency]))

(def lock (Object.))

(s/def ::compiled? boolean?)

(s/fdef file-details
  :args (s/cat :reader ::gio/reader
               :file ::gio/absolute-path)
  :ret ::config/file-details)

(defn- file-details
  [reader file]
  {:file file
   :text (gio/read-file reader file)})

(s/fdef unique-input-files
  :args (s/cat :reader ::gio/reader
               :input-file-globs (s/coll-of string?))
  :ret (s/coll-of string?))

(defn- unique-input-files
  [reader input-file-globs]
  (gio/expand-globs reader input-file-globs))

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

(s/fdef input-file?
  :args (s/cat :input-file-paths (s/coll-of ::gio/absolute-path :kind set?)
               :changed-file ::gio/absolute-path)
  :ret boolean?)

(defn- input-file?
  [input-file-paths changed-file]
  (contains? input-file-paths changed-file))

(s/fdef dependent-input-files
  :args (s/cat :dependency-graph ::builder/dependency-graph
               :changed-file ::gio/absolute-path)
  :ret (s/coll-of ::gio/absolute-path))

(defn- dependent-input-files
  [dependency-graph changed-file]
  (dependency/transitive-dependents dependency-graph changed-file))

(s/fdef get-files-to-recompile
  :args (s/cat :dependency-graph ::builder/dependency-graph
               :input-file-paths (s/coll-of ::gio/absolute-path :kind set?)
               :changed-file ::gio/absolute-path)
  :ret (s/coll-of ::gio/absolute-path :kind set?))

(defn- get-files-to-recompile
  [dependency-graph input-file-paths changed-file]
  (if (input-file? input-file-paths changed-file)
    #{changed-file}
    (dependent-input-files dependency-graph changed-file)))

(s/fdef recompile
  :args (s/cat :reader ::gio/reader
               :builder ::builder/builder
               :files-to-recompile (s/coll-of ::gio/absolute-path :kind set?)
               :file ::builder/output-file)
  ;; TODO: This return spec is not complete, doesn't contain the result
  :ret (s/and ::config/file-details
              (s/keys :opt-un [::compiled?])))

(defn- recompile
  [reader builder files-to-recompile file]
  (if-not (contains? files-to-recompile (:file file))
    file
    (let [new-file-details (file-details reader (:file file))
          compiled (builder/build-file builder
                                       new-file-details)]
      (assoc compiled :compiled? true))))

(defn- update-cached-files!
  [cache compiled-files]
  ;; TODO: Implement
  )

(defn- update-cached-dependency-graph!
  [cache compiled-files]
  ;; TODO: Implement
  )

(defn- handle-file-change
  [builder reader writer cache input-file-globs output-file changed-file]
  (locking lock
    (let [input-file-paths
          (set (unique-input-files reader input-file-globs))
          
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
      (output-compiled-files writer compiled-files output-file)
      ;; (locking lock
      ;;   ;; Changed file is one of the input files
      ;;   (when (contains? input-file-paths changed-file)
      ;;     (if (contains? (:files-by-name @cached-files))
      ;;       (logging/info (str "Detected new file: " changed-file))
      ;;       (logging/info (str "Detected file changes: " changed-file)))
      ;;     (swap! cached-files assoc-in [:files-by-name changed-file]
      ;;            (-> (file-details reader changed-file)
      ;;                (assoc :needs-recompile? true))))
      ;;   ;; Changed file is a dependency of one of the input files
      ;;   ;; Rewrite this part, get all the files that need recompile, and build them
      ;;   (let [file-info (file-details reader changed-file)
      ;;         compiled-file (builder/build-file builder file-info)]
      ;;     (swap! cached-files
      ;;            update :files-by-name
      ;;            assoc (:file compiled-file) compiled-file)
      ;;     (let [compiled-files (-> @cached-files :files-by-name vals)]
      ;;       (output-compiled-files writer compiled-files output-file))))
      ))
  (s/fdef init
    :args (s/cat :config ::config/config)))

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
  (let [input-file-globs (:input-files config)
        input-files (->> (unique-input-files reader input-file-globs)
                         (map #(file-details reader %)))
        output-file (gio/get-absolute-path reader (:output-file config))
        compiled-files (pmap #(builder/build-file builder %) input-files)]
    (output-compiled-files writer compiled-files output-file)))

(s/fdef watch
  :args (s/cat :builder ::builder/builder
               :watcher ::watcher/watcher
               :reader ::gio/reader
               :writer ::gio/writer
               :cached-files #(instance? clojure.lang.IDeref %)
               :config ::config/config))

(defn watch
  "Compiles the user's stylesheets on change."
  [builder watcher reader writer cached-files config]
  (let [input-file-globs (:input-files config)
        input-files (->> (unique-input-files reader input-file-globs)
                         (map #(file-details reader %)))
        output-file (gio/get-absolute-path reader (:output-file config))
        compiled-files (pmap #(builder/build-file builder %) input-files)
        files-by-name (utils/to-map :file compiled-files)
        dependency-graph (builder/get-dependency-graph builder compiled-files)]
    (output-compiled-files writer compiled-files output-file)
    (swap! cached-files assoc
           :files-by-name files-by-name
           :dependency-graph dependency-graph)
    (watcher/watch
     watcher
     ["."]
     #(handle-file-change builder reader writer cached-files
                          input-file-globs output-file %))))

