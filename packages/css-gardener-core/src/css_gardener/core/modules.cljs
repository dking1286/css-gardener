(ns css-gardener.core.modules
  (:require [integrant.core :as ig]))

(defmethod ig/init-key :load-module
  [_ _]
  (fn [module]
    (when (:node-module module)
      (js/require (:node-module module)))))