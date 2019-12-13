(ns xyz.dking.css-gardener.errors)

(defn not-started
  "Error indicating that a builder was not started correctly."
  ([message]
   (not-started message nil))
  ([message cause]
   (ex-info message {:type :not-started} cause)))
