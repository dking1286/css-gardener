(ns css-gardener.core.transformation
  (:require [clojure.core.async :refer [go]]
            [clojure.spec.alpha :as s]
            [css-gardener.core.logging :as logging]
            [css-gardener.core.modules :as modules]
            [goog.object :as gobj]
            [integrant.core :as ig]))

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
