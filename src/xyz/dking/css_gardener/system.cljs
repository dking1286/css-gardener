(ns xyz.dking.css-gardener.system
  (:require [com.stuartsierra.component :as component]
            [xyz.dking.css-gardener.logging :as logging]))

(defn system
  [{:keys [log-level]
    :or {log-level :info}}]
  (component/system-map
    :logger (logging/new-logger {:level log-level})))
