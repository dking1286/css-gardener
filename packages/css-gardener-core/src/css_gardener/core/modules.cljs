(ns css-gardener.core.modules
  (:require [integrant.core :as ig]))

(defmethod ig/init-key ::load
  [_ {:keys [return-value]}]
  (fn [module]
    (if return-value
      return-value
      (js/require (:node-module module)))))