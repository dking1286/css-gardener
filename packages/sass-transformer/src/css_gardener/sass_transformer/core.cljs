(ns css-gardener.sass-transformer.core)

(defn enter
  "Entry point for the sass transformer."
  [_ _ _]
  ;; TODO: Implement me
  )

(defn exit
  "Exit point for the sass transformer."
  [file _ callback]
  (callback nil file))