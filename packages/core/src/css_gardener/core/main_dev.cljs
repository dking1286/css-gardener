(ns
  ^{:clj-kondo/config {:linters {:unused-namespace {:level :off}
                                 :unused-binding {:level :off}}}}
  css-gardener.core.main-dev
  (:require [clojure.core.async :refer [go <!]]
            [clojure.edn :as edn]
            [clojure.pprint :refer [pprint]]
            [clojure.spec.test.alpha :as stest]
            [clojure.test :refer [run-tests run-all-tests]]
            [css-gardener.core.actions :as actions]
            [css-gardener.core.actions-test :as actions-test]
            [css-gardener.core.arguments-test :as arguments-test]
            [css-gardener.core.caching :as caching]
            [css-gardener.core.caching-test :as caching-test]
            [css-gardener.core.change-detection :as changes]
            [css-gardener.core.change-detection-test :as change-detection-test]
            [css-gardener.core.cljs-parsing-test :as cljs-parsing-test]
            [css-gardener.core.config :as config]
            [css-gardener.core.config-test :as config-test]
            [css-gardener.core.dependency :as dependency]
            [css-gardener.core.dependency-test :as dependency-test]
            [css-gardener.core.file-test :as file-test]
            [css-gardener.core.logging :as logging]
            [css-gardener.core.main]
            [css-gardener.core.output :as output]
            [css-gardener.core.system :as system]
            [css-gardener.core.transformation :as transformation]
            [css-gardener.core.transformation-test :as transformation-test]
            [css-gardener.core.utils.async :as a]
            [css-gardener.core.utils.async-test :as async-test]
            [css-gardener.core.utils.errors :as errors]
            [css-gardener.core.utils.fs-test :as fs-test]
            [fs]
            [integrant.core :as ig]))

(defonce ^:private system (atom nil))

(defn- start
  [sys-config]
  (when-not @system
    (println "Starting system")
    (reset! system (ig/init sys-config))))

(defn- stop
  []
  (when @system
    (println "Stopping system")
    (ig/halt! @system)
    (reset! system nil)))

(defn- add-global-error-handling
  "Adds global error handlers to the nodejs process. This is necessary
   becase tests can sometimes throw errors in async processes, so they cannot
   be caught with a normal try-catch block, in which case the repl process
   will shut down when running such a test from the dev build."
  []
  (println "Adding global error handling")
  (.on js/process "uncaughtException"
       (fn [err]
         (println "An uncaught error occurred:")
         (println err)
         (println (.-stack err))))
  (.on js/process "unhandledRejection"
       (fn [err]
         (println "An undandled rejection occurred:")
         (println err)
         (println (.-stack err)))))

(defn ^:dev/before-load before-load
  "Lifecycle function that is called before new code is loaded in development."
  []
  (println "Reloading..."))

(defn ^:dev/after-load after-load
  "Lifecycle function that is called after new code is loaded in development."
  []
  ;; (run-tests 'css-gardener.core.transformation-test)
  (run-all-tests)
  ;
  )

(defn main
  "Entry point for the css-gardener process in development."
  [& _]
  (add-global-error-handling)
  (after-load))

(comment
  "Evaluate this form to run all tests in all namespaces transitively
   required by this one."
  (run-all-tests)

  "If you are going to run the system against one of the example projects,
   before you start the system, evaluate these forms to change the working
   directory of the repl into the example directory."
  (js/process.chdir "../css-example")
  (js/process.cwd)

  "Next, evaluate this to create the system config."
  (def ^:private sys-config
    (-> system/config
        (assoc ::config/config (edn/read-string (fs/readFileSync "css-gardener.edn" "utf8")))
        (assoc-in [::logging/logger :level] :debug)
        (assoc-in [::changes/watcher :source-paths] ["src"])))

  (pprint sys-config)

  "Evaluate these functions in the repl to start and stop the system for
   development."
  (stop)
  (start sys-config)

  "More forms to evaluate for development"
  (let [deps-graph (::dependency/deps-graph @system)
        compile-all (::transformation/compile-all @system)
        build-id :app]
    (go
      (let [output-or-error (<! (->> (deps-graph build-id)
                                     (a/trace "[dependency graph]")
                                     (a/flat-map #(compile-all build-id %))
                                     (a/trace "[compiled files]")))]
        (println "The output: ")
        (pprint output-or-error)))))