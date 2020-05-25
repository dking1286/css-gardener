(ns css-gardener.core.transformation
  (:require [clojure.core.async :refer [go]]
            [clojure.spec.alpha :as s]
            [css-gardener.core.config :as config]
            [css-gardener.core.file :as file]
            [css-gardener.core.logging :as logging]
            [css-gardener.core.modules :as modules]
            [css-gardener.core.utils.async :as a]
            [css-gardener.core.utils.errors :as errors]
            [css-gardener.core.utils.js :refer [to-js from-js]]
            [goog.object :as gobj]
            [integrant.core :as ig]))

;; ::transformers ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn transformer?
  "Determines whether or not a value is a valid transformer."
  [val]
  (boolean (and (gobj/get val "enter")
                (gobj/get val "exit"))))

(s/def ::transformer transformer?)
(s/def ::options map?)
(s/def ::transformer-config
  (s/keys :req-un [::modules/module ::transformer]))

(defn transformer-stub
  "Creates a stub transformer for use in tests."
  [err result]
  #js {:enter (fn [_ _ callback]
                (go (callback err result)))
       :exit (fn [_ _ callback]
               (go (callback err result)))})

(defmethod ig/init-key ::transformers
  [_ {:keys [config logger load-module]}]
  (logging/debug logger "Loading transformers")
  (->> (:rules config)
       vals
       (mapcat :transformers)
       (map (fn [config]
              (let [module (modules/extract-module config)]
                {:module module
                 :transformer (load-module module)})))
       (map #(vector (:module %) %))
       (into {})))

;; ::transform ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- get-transformers-stack
  [transformers transformers-rule]
  (->> transformers-rule
       (map #(assoc (get transformers (modules/extract-module %))
                    :options (:options %)))))

(defn- get-transformer-functions
  [transformers-stack]
  (concat (map #(assoc % :function (gobj/get (:transformer %) "enter"))
               transformers-stack)
          (reverse (map #(assoc % :function (gobj/get (:transformer %) "exit"))
                        transformers-stack))))

(defn- apply-transformer-function
  [{:keys [function options module]} file]
  (a/callback->channel
   function
   file
   (to-js options)
   (fn [err new-file]
     (if err
       (errors/unexpected-error (str "Error in transformer " module)
                                err)
       new-file))))

(s/fdef transform
  :args (s/cat :config ::config/config
               :transformers (s/map-of ::modules/module ::transformer-config)
               :file ::file/file))

(defn- transform
  "Transforms a file using the transformers specified in the config.
   
   Each transformer's :enter method is called in order, and then each
   transformer's :exit method is called in reverse order."
  [;; Injected dependencies
   config transformers
   ;; Arguments
   file]
  (let [rule-or-error (config/matching-rule config file)]
    (if (errors/error? rule-or-error)
      (go (errors/invalid-config (str "Problem finding rule for file "
                                      (:absolute-path file))
                                 rule-or-error))
      (let [transformers-stack (get-transformers-stack
                                transformers (:transformers rule-or-error))
            functions (get-transformer-functions transformers-stack)]
        (loop [result (go (to-js file))
               remaining-functions functions]
          (if (empty? remaining-functions)
            (a/map from-js result)
            (let [func (first remaining-functions)]
              (recur (a/flat-map #(apply-transformer-function func %) result)
                     (rest remaining-functions)))))))))

(defmethod ig/init-key ::transform
  [_ {:keys [config transformers]}]
  (partial transform config transformers))