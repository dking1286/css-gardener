(ns css-gardener.css-transformer.main)

(defn transform
  [file]
  (js/Promise.resolve file))