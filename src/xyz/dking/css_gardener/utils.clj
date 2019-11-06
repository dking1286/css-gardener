(ns xyz.dking.css-gardener.utils)

(defn map-vals
  "Maps over a seq of key-value pairs, applying the mapping function to
  each value."
  [f & args]
  (apply map (fn [[key val]] [key (f val)]) args))
