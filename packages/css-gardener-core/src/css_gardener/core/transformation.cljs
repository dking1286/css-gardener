(ns css-gardener.core.transformation
  (:require [clojure.core.async :refer [go]]
            [css-gardener.core.logging :as logging]
            [css-gardener.core.modules :as modules]
            [integrant.core :as ig]))

(defn transformer-stub
  "Creates a stub transformer for use in tests."
  [err result]
  (fn [_ _ callback]
    ;; Call the callback asynchronously so that tests can't rely on it being
    ;; synchronous
    (go (callback err result))))

(defmethod ig/init-key ::transformers
  [_ {:keys [config logger load-module]}]
  (logging/debug logger "Loading transformers")
  (->> (:rules config)
       vals
       (mapcat :transformers)
       (map modules/extract-module)
       (map #(vector % (load-module %)))
       (into {})))
