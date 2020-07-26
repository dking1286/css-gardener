(ns css-gardener.core.modules
  (:require [clojure.spec.alpha :as s]
            [css-gardener.common.node-modules :as node-modules]
            [css-gardener.core.utils.errors :as errors]
            [integrant.core :as ig]
            [path]))

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

(defmulti load-module
  "Loads a module from a ::module data structure."
  (fn [module-type _] module-type))

(defmethod load-module :node
  [_ module]
  ;; Calling require with the module path would look for the module in the
  ;; dependencies of @css-gardener/core. Instead, we want to search for the
  ;; module in the node_modules of the project that is *using*
  ;; @css-gardener/core. Therefore, construct the path relative to the
  ;; current working directory.
  (js/require (node-modules/root-module-path (:node-module module))))

(defmethod load-module :default
  [_ module]
  (throw (errors/invalid-config (str "Unsupported module format: "
                                     module))))

(defmethod ig/init-key ::load
  [_ {:keys [modules]}]
  (fn [module]
    (if (contains? modules module)
      (get modules module)
      (let [conformed (s/conform ::module module)]
        (if (= ::s/invalid conformed)
          (throw (errors/invalid-config (str "Invalid module "
                                             module
                                             " found in config. "
                                             (s/explain-data ::module module))))
          (let [[module-type module] conformed]
            (load-module module-type module)))))))
