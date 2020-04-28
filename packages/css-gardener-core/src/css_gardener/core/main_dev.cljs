(ns css-gardener.core.main-dev
  (:require [clojure.spec.test.alpha :as stest]
            [css-gardener.core.main]
            [css-gardener.core.system :as system]
            [integrant.core :as ig]))

(def system (atom nil))

(defn start
  []
  (when-not @system
    (reset! system (ig/init system/config))))

(defn stop
  []
  (when @system
    (ig/halt! @system)
    (reset! system nil)))

(defn ^:dev/before-load before-load
  []
  (println "Stopping system...")
  (stop)
  (stest/unstrument)
  (println "Reloading..."))

(defn ^:dev/after-load after-load
  []
  (println "Starting system...")
  (stest/instrument)
  (start)
  (println "done!"))

(defn main
  [& _]
  (after-load))