(ns css-gardener.css-transformer.main)

(defn enter
  [file]
  (js/Promise.resolve file))

(defn exit
  [file]
  (js/Promise.resolve file))