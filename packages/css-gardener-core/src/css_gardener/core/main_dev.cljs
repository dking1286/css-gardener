(ns css-gardener.core.main-dev
  (:require [clojure.core.async :refer [go <!]]
            [clojure.edn :as edn]
            [clojure.pprint :refer [pprint]]
            [clojure.spec.test.alpha :as stest]
            [css-gardener.core.change-detection :as changes]
            [css-gardener.core.dependency :as dependency]
            [css-gardener.core.logging :as logging]
            [css-gardener.core.main]
            [css-gardener.core.system :as system]
            [css-gardener.core.utils.errors :as errors]
            [fs]
            [integrant.core :as ig]))

(defonce ^:private system (atom nil))

(def ^:private sys-config
  (-> system/config
      (assoc-in [::logging/logger :level] :debug)
      (assoc-in [::changes/watcher :source-paths] ["src"])))

(defn- start
  []
  (when-not @system
    (println "Starting system")
    (reset! system (ig/init sys-config))))

(defn- stop
  []
  (when @system
    (println "Stopping system")
    (ig/halt! @system)
    (reset! system nil)))

(comment
  "If you are going to run the system against one of the example projects,
   before you start the system, evaluate these forms to change the working
   directory of the repl into the example directory."
  (js/process.chdir "../css-example")
  (js/process.cwd)

  "Evaluate these functions in the repl to start and stop the system for
   development."
  (stop)
  (start)

  "More forms to evaluate for development"
  (let [logger (::logging/logger @system)
        deps-graph (::dependency/deps-graph @system)
        consume-changes (::changes/consumer @system)
        config (edn/read-string (fs/readFileSync "css-gardener.edn" "utf8"))
        build-id :app]
    (go
      (let [graph-or-error (<! (deps-graph config build-id))]
        (if (errors/error? graph-or-error)
          (do
            (logging/error logger (errors/message graph-or-error))
            (logging/error logger (errors/stack graph-or-error)))
          (do
            (pprint graph-or-error)
            (consume-changes)))))))

(defn ^:dev/before-load before-load
  "Lifecycle function that is called before new code is loaded in development."
  []
  (stest/unstrument)
  (println "Reloading..."))

(defn ^:dev/after-load after-load
  "Lifecycle function that is called after new code is loaded in development."
  []
  (stest/instrument)
  (println "done!"))

(defn main
  "Entry point for the css-gardener process in development."
  [& _]
  (after-load))