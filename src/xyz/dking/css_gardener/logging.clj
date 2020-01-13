(ns xyz.dking.css-gardener.logging)

(def log-levels {:debug 0 :info 1 :warn 2 :error 3})

(def log-level (atom :info))

(defn log-fn
  [level]
  (fn [message]
    (when (<= (log-levels @log-level) (log-levels level))
      (println message))))

(def debug (log-fn :debug))
(def info (log-fn :info))
(def warn (log-fn :warn))
(def error (log-fn :error))

