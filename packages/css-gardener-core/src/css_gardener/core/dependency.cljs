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
  :args (s/cat :load-module fn?
               :rule ::config/rule))

(defn- get-resolver
  [load-module dependency-resolver]
  (go
    (try
      (assoc dependency-resolver :resolver (load-module dependency-resolver))
      (catch js/Error err
        (errors/invalid-dependency-resolver "Failed to load module" err)))))

(defn- resolve-deps
  [resolver resolver-name file]
  (a/node-callback->channel
   resolver file (fn [err deps]
                   (cond
                     err err

                     (js/Array.isArray deps) deps

                     :else
                     (errors/invalid-dependency-resolver
                      (str "Expected dependency resolver " resolver-name
                           " to yield an array of strings, got "
                           deps))))))

(s/fdef deps
  :args (s/cat :load-module fn?
               :cljs-deps fn?
               :file ::file/file
               :config ::config/config))

(defn- deps
  [load-module cljs-deps file config]
  (let [rule-or-error (config/matching-rule config file)]
    (cond
      (cljs/cljs-file? file)
      (cljs-deps file (:source-paths config))

      (errors/not-found? rule-or-error)
      (go (errors/invalid-config (str "Problem finding rule for file "
                                      (:absolute-path file))
                                 rule-or-error))

      (errors/conflict? rule-or-error)
      (go (errors/invalid-config (str "Problem finding rule for file "
                                      (:absolute-path file))
                                 rule-or-error))

      (not (:dependency-resolver rule-or-error))
      (go #{})

      :else
      (->> (get-resolver load-module (:dependency-resolver rule-or-error))
           (a/flat-map #(resolve-deps (:resolver %) (:node-module %) file))
           (a/map set)))))

(defmethod ig/init-key ::deps
  [_ {:keys [load-module cljs-deps]}]
  (partial deps load-module cljs-deps))
