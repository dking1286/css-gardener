(ns xyz.dking.css-gardener.v1.main
  (:require [xyz.dking.css-gardener.v1.args]))

(declare RUN_MAIN)

(goog-define RUN_MAIN false)

(defn ^:dev/before-load before-load
  []
  (print "Reloading..."))

(defn ^:dev/after-load after-load
  []
  (println "Done!"))

(defn main
  [& _]
  (if RUN_MAIN
    (println "running the main process")
    (println "Node repl started!")))
