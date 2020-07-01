(ns css-gardener.core.main
  (:require [clojure.core.async :refer [go go-loop <!]]
            [clojure.edn :as edn]
            [clojure.spec.alpha :as s]
            [css-gardener.core.actions :as actions]
            [css-gardener.core.arguments :as arguments]
            [css-gardener.core.caching :as caching]
            [css-gardener.core.change-detection :as changes]
            [css-gardener.core.config :as config]
            [css-gardener.core.dependency :as dependency]
            [css-gardener.core.file :as file]
            [css-gardener.core.logging :as logging]
            [css-gardener.core.output :as output]
            [css-gardener.core.system :as system]
            [css-gardener.core.utils.errors :as errors]
            [css-gardener.core.utils.fs :as fs-utils]
            [fs]
            [integrant.core :as ig]
            [path]))

(defn- get-config
  [options]
  (or (:config options)
      (-> (:config-file options)
          (fs/readFileSync "utf8")
          edn/read-string)))

(defn- normalize-config
  [config]
  (if (:infer-source-paths-and-builds config)
    (let [other-config (-> (:infer-source-paths-and-builds config)
                           (fs/readFileSync "utf8")
                           edn/read-string)]
      ;; TODO: Make this support config types other than shadow-cljs
      (assoc config
             :source-paths (:source-paths other-config)
             :builds (:builds other-config)))
    config))

(defn- validate-config
  [config]
  (when-not (s/valid? ::config/config config)
    (throw (errors/invalid-config
            (str "Invalid configuration: "
                 (s/explain-data ::config/config config)))))
  config)

(defn- start-system
  [sys-config]
  (try
    (ig/init sys-config)
    (catch js/Error err
      (println "An error occurred while starting the system: ")
      (println err)
      (throw err))))

(defn- watch
  [config build-id log-level]
  (let [sys-config
        (-> system/config
            (assoc ::config/config config)
            (assoc ::arguments/command :watch)
            (assoc-in [::arguments/build-id :id] build-id)
            (assoc-in [::logging/logger :level] log-level)
            (assoc-in [::changes/watcher :watch?] true))

        {logger ::logging/logger
         input-channel ::changes/input-channel
         output-channel ::output/output-channel
         dependency-graph-cache ::caching/dependency-graph-cache
         deps ::dependency/deps
         read-file ::fs-utils/read-file
         :as system}
        (start-system sys-config)]
    (actions/apply-all system [(actions/->CreateDependencyGraph)
                               (actions/->CompileOnce)])
    (logging/debug logger "Starting change consumer")
    (go-loop []
      (let [value (<! input-channel)]
        (if (nil? value)
          (logging/debug logger "Input channel closed, stopping consumer.")
          (do
            (logging/info logger (str "Detected changes: " value))
            ;; TODO: Make changes to handle adding and removing files
            (let [absolute-path (path/resolve (:path value))
                  file (<! (file/from-path read-file absolute-path))
                  new-deps (<! (deps file))
                  actions (changes/get-actions config
                                               @dependency-graph-cache
                                               absolute-path
                                               new-deps)]
              (<! (actions/apply-all system actions))
              (recur))))))
    (logging/debug logger "Starting output consumer")
    (go-loop []
      (let [value (<! output-channel)]
        (if (nil? value)
          (logging/debug logger "Output channel closed, stopping consumer.")
          (do
            (<! (output/write-output logger value))
            (recur)))))))

(defn- compile
  "Compiles the output stylesheet once, without applying optimizations."
  [config build-id log-level]
  (let [sys-config
        (-> system/config
            (assoc ::config/config config)
            (assoc ::arguments/command :compile)
            (assoc-in [::arguments/build-id :id] build-id)
            (assoc-in [::logging/logger :level] log-level))

        system
        (start-system sys-config)]
    (actions/apply-all system [(actions/->CreateDependencyGraph)
                               (actions/->CompileOnce)])))

(defn- release
  "TODO: Implement me"
  [_ _ _])

(defn- main
  "Main function for the css-gardener process."
  [& args]
  (let [{[command build-id] :arguments
         {:keys [help log-level] :as options} :options
         summary :summary
         errors :errors} (arguments/parse args)]
    (cond
      ;; Print the summary if the user asked for help
      help (println summary)
      ;; Show an error if the args were invalid
      errors (println (first errors))
      ;; Otherwise proceed
      :else
      (let [config (-> (get-config options)
                       normalize-config
                       validate-config)]
        (case command
          :watch (watch config build-id log-level)
          :compile (go
                     (let [error (<! (compile config build-id log-level))]
                       (js/process.exit (if error 1 0))))
          :release (release config build-id log-level)
          (throw (js/Error. (str "Invalid command " command))))))))

(defn entry
  "Entry point for the css-gardener process."
  []
  (apply main (.slice js/process.argv 2)))
