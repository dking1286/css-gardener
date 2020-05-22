(ns css-gardener.core.modules
  (:require [integrant.core :as ig]))

(defmethod ig/init-key ::load
  [_ {:keys [modules]}]
  (fn [module]
    (cond
      ;; Map of mock return values
      (contains? modules module) (get modules module)
      ;; Real implementation
      :else (js/require (:node-module module)))))