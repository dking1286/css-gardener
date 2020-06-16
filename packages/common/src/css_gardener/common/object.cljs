(ns css-gardener.common.object)

(defn object-merge
  "Merges two or more Javascript objects."
  [& objects]
  (apply js/Object.assign #js {} objects))