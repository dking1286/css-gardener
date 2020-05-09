(ns css-gardener.core.logging
  (:require [clojure.pprint :refer [pprint]]
            [clojure.spec.alpha :as s]
            [goog.log :as log]
            [integrant.core :as ig])
  (:import [goog.log Level Logger]))

(def goog-level
  {:debug Level/FINE
   :info Level/INFO
   :warning Level/WARNING
   :error Level/SEVERE})

(s/def ::sink #{:console :cache})
(s/def ::level (set (keys goog-level)))
(s/def ::goog-logger #(instance? Logger %))
(s/def ::cache #(satisfies? IDeref %))
(s/def ::handler fn?)
(s/def ::handlers (s/coll-of ::handler))
(s/def ::logger (s/keys :req-un [::goog-logger ::cache ::handlers]))

(s/def ::log-fn-args
  (s/cat :logger ::logger
         :msg string?))

(defn log-fn
  [f]
  (fn [logger msg]
    (f (:goog-logger logger) msg)))

(s/fdef debug
  :args ::log-fn-args)

(def debug (log-fn log/fine))

(s/fdef info
  :args ::log-fn-args)

(def info (log-fn log/info))

(s/fdef warning
  :args ::log-fn-args)

(def warning (log-fn log/warning))

(s/fdef error
  :args ::log-fn-args)

(def error (log-fn log/error))

(defmethod ig/init-key ::logger
  [_ {:keys [sinks level]
      :or {sinks #{} level :info}}]
  (let [level (goog-level level)
        goog-logger (log/getLogger "css-gardener.core.logging" level)
        cache (atom [])
        console-handler #(pprint (.getMessage %))
        cache-handler #(swap! cache conj %)]
    (when (contains? sinks :console)
      (log/addHandler goog-logger console-handler))
    (when (contains? sinks :cache)
      (log/addHandler goog-logger cache-handler))
    {:goog-logger goog-logger
     :cache cache
     :handlers [console-handler cache-handler]}))

(defmethod ig/halt-key! ::logger
  [_ {:keys [goog-logger cache handlers]}]
  (reset! cache [])
  (doseq [handler handlers]
    (log/removeHandler goog-logger handler)))
