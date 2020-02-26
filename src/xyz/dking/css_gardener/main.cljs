(ns xyz.dking.css-gardener.main)

(declare RUN_MAIN)

(goog-define RUN_MAIN false)

(defn ^:dev/before-load before-load
  []
  (print "Reloading..."))

(defn ^:dev/after-load after-load
  []
  (println "done!"))

(defn main
  [& args]
  (if RUN_MAIN
    (println "running the main process")
    (println "Node repl started!")))