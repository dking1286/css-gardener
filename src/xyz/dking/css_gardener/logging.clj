(ns xyz.dking.css-gardener.logging)

(defn warn
  "Logs a warning to the current *out* writer."
  [message]
  (println (str "[WARN] " message)))
