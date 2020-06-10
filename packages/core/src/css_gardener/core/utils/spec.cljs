(ns css-gardener.core.utils.spec
  (:require [clojure.spec.alpha :as s]))

(defn deref-of
  "Creates a spec that determines if a value is a deref-able whose inner
   value satisfies a spec."
  [inner]
  (fn [thing]
    (and (satisfies? IDeref thing)
         (s/valid? inner @thing))))