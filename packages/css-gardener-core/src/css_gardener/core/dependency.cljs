(ns css-gardener.core.dependency
  (:require [clojure.core.async :refer [go]]
            [clojure.spec.alpha :as s]
            [css-gardener.core.cljs-parsing :as cljs]
            [css-gardener.core.config :as config]
            [css-gardener.core.file :as file]
            [css-gardener.core.utils.async :as a]
            [css-gardener.core.utils.errors :as errors]
            [integrant.core :as ig]))

(s/fdef get-resolver
  :args (s/cat :rule ::config/rule))

(defn- get-resolver
  [load-module {:keys [dependency-resolver]}]
  (when dependency-resolver
    (load-module (:node-module dependency-resolver))))

(defn- resolve-deps
  [resolver file]
  (if-not resolver
    #{}
    (a/node-callback->channel resolver file (fn [err deps] (or err deps)))))

(s/fdef deps
  :args (s/cat :file ::file/file
               :config ::config/config))

(defn- deps
  [load-module cljs-deps file config]
  (if (cljs/cljs-file? file)
    (cljs-deps file (:source-paths config))
    (let [rule (config/matching-rule config file)]
      (if-not rule
        (go (errors/invalid-config (str "No rule found matching file "
                                      (:absolute-path file))))
        (let [resolver (get-resolver load-module rule)]
          (resolve-deps resolver file))))))

(defmethod ig/init-key :deps
  [_ {:keys [load-module cljs-deps]}]
  (partial deps load-module cljs-deps))