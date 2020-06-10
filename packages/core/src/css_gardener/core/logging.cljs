(ns css-gardener.core.logging
  (:require [clojure.spec.alpha :as s]
            [goog.log :as log]
            [integrant.core :as ig])
  (:import [goog.log Level Logger]))

(def ^:private root-logger-name "css-gardener.core.logging")

(defn- logger-name
  []
  (if (identical? js/goog.DEBUG true)
    (str root-logger-name "-" (.toString (random-uuid)))
    root-logger-name))

(def ^{:doc "Map of log level keywords to the corresponding constants in the
             Closure library logging framework."}
  goog-level
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

(defn- log-fn
  [f]
  (fn [logger msg]
    (f (:goog-logger logger) msg)))

(s/fdef debug
  :args ::log-fn-args)

(def ^{:doc "Log at the :debug level"}
  debug
  (log-fn log/fine))

(s/fdef info
  :args ::log-fn-args)

(def ^{:doc "Log at the :info level"}
  info
  (log-fn log/info))

(s/fdef warning
  :args ::log-fn-args)

(def ^{:doc "Log at the :warning level"}
  warning
  (log-fn log/warning))

(s/fdef error
  :args ::log-fn-args)

(def ^{:doc "Log at the :error level"}
  error
  (log-fn log/error))

(defn has-message?
  "Determines if a message has been logged matching a predicate."
  [logger pred]
  (seq (->> (:cache logger)
            deref
            (filter pred))))

(defmethod ig/init-key ::logger
  [_ {:keys [sinks level]
      :or {sinks #{} level :info}}]
  (let [level (goog-level level)
        goog-logger (log/getLogger (logger-name) level)
        cache (atom [])
        console-handler #(println (.getMessage %))
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
