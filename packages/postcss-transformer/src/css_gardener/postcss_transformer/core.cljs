(ns css-gardener.postcss-transformer.core)

(defn enter
  [file _ callback]
  (callback nil file))

(defn exit
  [file _ callback]
  (callback nil file))