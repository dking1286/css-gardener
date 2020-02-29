(ns xyz.dking.css-gardener.main
  (:require [orchestra-cljs.spec.test :as st]
            [xyz.dking.css-gardener.system]))

(declare RUN_MAIN)

(goog-define RUN_MAIN false)

(defn start-dev
  []
  (println "Instrumenting specs...")
  (st/instrument))

(defn stop-dev
  []
  (println "Uninstrumenting specs...")
  (st/unstrument))

(defn ^:dev/before-load before-load
  []
  (stop-dev)
  (print "Reloading..."))

(defn ^:dev/after-load after-load
  []
  (start-dev)
  (println "done!"))

(defn main
  [& _]
  (if RUN_MAIN
    (println "running the main process")
    (do
      (start-dev)
      (println "Node repl started!"))))