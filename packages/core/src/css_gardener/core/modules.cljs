(ns css-gardener.core.modules
  (:require [clojure.spec.alpha :as s]
            [css-gardener.core.utils.errors :as errors]
            [integrant.core :as ig]))

(s/def ::node-module string?)
(s/def ::module (s/or :node (s/keys :req-un [::node-module])))

(s/fdef extract-module
  :args (s/cat :module ::module)
  :ret ::module)

(defn extract-module
  "Takes a module configuration map, and strips off the :options key.
   
   This is necessary because the same module may appear more than once in the
   config, with different options. When constructing a map of dependency
   resolvers or transformers, these should be treated as the same key."
  [module]
  (dissoc module :options))

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
