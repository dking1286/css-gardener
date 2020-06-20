(ns css-gardener.core.main
  (:require [clojure.core.async :refer [go <!]]
            [clojure.edn :as edn]
            [clojure.spec.alpha :as s]
            [css-gardener.core.arguments :as arguments]
            [css-gardener.core.change-detection :as changes]
            [css-gardener.core.config :as config]
            [css-gardener.core.dependency :as dependency]
            [css-gardener.core.logging :as logging]
            [css-gardener.core.output :as output]
            [css-gardener.core.system :as system]
            [css-gardener.core.transformation :as transformation]
            [css-gardener.core.utils.async :as a]
            [css-gardener.core.utils.errors :as errors]
            [fs]
            [integrant.core :as ig]))

(defn- read-file-sync
  [path]
  (fs/readFileSync path "utf8"))

(defn- get-config
  [options]
  (or (:config options)
      (-> (:config-file options)
          read-file-sync
          edn/read-string)))

(defn- normalize-config
  [config]
  (if (:infer-source-paths-and-builds config)
    (let [other-config (-> (:infer-source-paths-and-builds config)
                           read-file-sync
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

(defn- watch
  [config build-id log-level]
  (let [source-paths (:source-paths config)
        sys-config (-> system/config
                       (assoc-in [::changes/watcher :source-paths] source-paths)
                       (assoc-in [::logging/logger :level] log-level))
        system (ig/init sys-config)
        logger (::logging/logger system)
        deps-graph (::dependency/deps-graph system)
        consume-changes (::changes/consumer system)]
    (go
      (let [graph-or-error (<! (deps-graph config build-id))]
        (if (errors/error? graph-or-error)
          (do
            (logging/error logger (errors/message graph-or-error))
            (logging/error logger (errors/stack graph-or-error)))
          (do
            (println graph-or-error)
            (consume-changes)))))))

(defn- compile
  "Compiles the output stylesheet once, without applying optimizations."
  [config build-id log-level]
  (let [sys-config (-> system/config
                       (assoc ::config/config config)
                       (assoc-in [::changes/watcher :source-paths]
                                 (:source-paths config))
                       (assoc-in [::logging/logger :level] log-level))
        system (try
                 (ig/init sys-config)
                 (catch js/Error err
                   (println "An error occurred while starting the system: ")
                   (println err)
                   (throw err)))
        {logger ::logging/logger
         deps-graph ::dependency/deps-graph
         compile-all ::transformation/compile-all
         write-output ::output/write-output} system]
    (go
      (let [output-or-error
            (<! (->> (deps-graph build-id)
                     (a/flat-map #(compile-all build-id %))
                     (a/flat-map #(->> %
                                       (map write-output)
                                       (a/await-all 5000)))))]
        (when (errors/error? output-or-error)
          (logging/error logger "An error occurred")
          (logging/error logger output-or-error)
          output-or-error)))))

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
