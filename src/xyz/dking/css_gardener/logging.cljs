(ns xyz.dking.css-gardener.logging
  (:require [cljs.spec.alpha :as s]))

(defprotocol ILogger
  (debug [logger message])
  (info [logger message])
  (warn [logger message])
  (error [logger message]))

(s/def ::log-level
  #{:debug :info :warn :error})

(def log-levels
  {:debug 1
   :info 2
   :warn 3
   :error 4})

(defn greater-or-equal
  [level1 level2]
  (>= (log-levels level1) (log-levels level2)))

(s/fdef greater-or-equal
        :args (s/cat :level1 ::log-level
                     :level2 ::log-level)
        :ret boolean?)

(defrecord Logger [level]
  ILogger
  (debug [_ message]
    (when (greater-or-equal :debug level)
      (println message)))
  (info [_ message]
    (when (greater-or-equal :info level)
      (println message)))
  (warn [_ message]
    (when (greater-or-equal :warn level)
      (println message)))
  (error [_ message]
    (when (greater-or-equal :error level)
      (println message))))

(defn new-logger
  [{:keys [level]}]
  (map->Logger {:level level}))