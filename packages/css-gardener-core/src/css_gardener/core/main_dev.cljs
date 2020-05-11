(ns css-gardener.core.main-dev
  (:require [clojure.spec.test.alpha :as stest]
            [css-gardener.core.main]
            [css-gardener.core.system :as system]
            [integrant.core :as ig]))

(def ^:private system (atom nil))

(defn- start
  []
  (when-not @system
    (reset! system (ig/init system/config))))

(defn- stop
  []
  (when @system
    (ig/halt! @system)
    (reset! system nil)))

(defn ^:dev/before-load before-load
  "Lifecycle function that is called before new code is loaded in development."
  []
  (println "Stopping system...")
  (stop)
  (stest/unstrument)
  (println "Reloading..."))

(defn ^:dev/after-load after-load
  "Lifecycle function that is called after new code is loaded in development."
  []
  (println "Starting system...")
  (stest/instrument)
  (start)
  (println "done!"))

(defn main
  "Entry point for the css-gardener process in development."
  [& _]
  (after-load))