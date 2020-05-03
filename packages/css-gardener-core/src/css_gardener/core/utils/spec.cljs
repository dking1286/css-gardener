(ns css-gardener.core.utils.spec
  (:require [clojure.spec.alpha :as s]))

(defn deref-of
  [inner]
  (fn [thing]
    (and (satisfies? IDeref thing)
         (s/valid? inner thing))))