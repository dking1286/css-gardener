(ns xyz.dking.css-gardener.watcher
  (:require [hawk.core :as hawk]
            [clojure.spec.alpha :as s]))

(declare Watcher)

(s/def ::watcher #(satisfies? Watcher %))

(defprotocol Watcher
  (watch [this paths callback])
  (stop [this]))

(defrecord HawkWatcher [watcher]
  Watcher
  (watch [this paths callback]
    (let [w (hawk/watch! [{:paths paths
                           :handler (fn [_ {:keys [file]}]
                                      (callback (.getAbsolutePath file)))}])]
      (reset! watcher w)))
  
  (stop [this]
    (when @watcher
      (hawk/stop! @watcher)
      (reset! watcher nil))))

(defn hawk-watcher
  []
  (->HawkWatcher (atom nil)))
