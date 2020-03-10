(ns xyz.dking.css-gardener.watcher
  (:require [clojure.core.async :refer [go >! put! chan close!]]
            [com.stuartsierra.component :as component]))

(defprotocol IWatcher
  (watch [this paths]))

(defn- start-watcher
  [watcher]
  (assoc watcher :channel (chan)))

(defn- stop-watcher
  [watcher]
  (let [channel (:channel watcher)]
    (when channel
      (close! channel)
      (dissoc watcher :channel))))

(defn- channel-or-error
  [watcher]
  (let [channel (:channel watcher)]
    (when-not channel
      (throw (ex-info "Cannot call watch before watcher is started."
                      {:type :watch-before-start})))
    channel))

(defrecord StubWatcher []
  component/Lifecycle
  (start [this] (start-watcher this))
  (stop [this] (stop-watcher this))
  IWatcher
  (watch [this _] (channel-or-error this)))

(defn new-stub-watcher
  []
  (->StubWatcher))

(defn trigger-change!
  [stub-watcher path]
  (put! (:channel stub-watcher) path))

(defrecord FileSystemWatcher []
  component/Lifecycle
  (start [this] (start-watcher this))
  (stop [this] (stop-watcher this))
  IWatcher
  (watch [this paths]
    (let [channel (channel-or-error this)]
      channel)))

(defn new-file-system-watcher
  []
  (->FileSystemWatcher))