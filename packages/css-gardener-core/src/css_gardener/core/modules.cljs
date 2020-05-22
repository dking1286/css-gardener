(ns css-gardener.core.modules
  (:require [clojure.spec.alpha :as s]
            [css-gardener.core.utils.errors :as errors]
            [integrant.core :as ig]))

(s/def ::node-module string?)
(s/def ::module (s/or :node (s/keys :req-un [::node-module])))

(defmethod ig/init-key ::load
  [_ {:keys [modules]}]
  (fn [module]
    (if (contains? modules module)
      (get modules module)
      (let [conformed (s/conform ::module module)]
        (if (= ::s/invalid conformed)
          (throw (errors/invalid-config (str "Invalid module "
                                             module
                                             " found in config")))
          (let [[module-type module] conformed]
            (case module-type
              :node (js/require (:node-module module)))))))))