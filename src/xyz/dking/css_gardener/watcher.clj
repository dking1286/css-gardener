(ns xyz.dking.css-gardener.watcher
  (:require [hawk.core :as hawk]))

(defprotocol Watcher
  (watch [this paths callback]))

(defrecord HawkWatcher []
  Watcher
  (watch [this paths callback]
    (hawk/watch! [{:paths paths
                   :handler (fn [_ {:keys [file]}]
                              (callback (.getAbsolutePath file)))}])))
