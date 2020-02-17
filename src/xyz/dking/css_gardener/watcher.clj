(ns xyz.dking.css-gardener.watcher
  (:require [hawk.core :as hawk]
            [clojure.spec.alpha :as s])
  (:import [java.io File]))

(declare Watcher)

;; Watcher protocol ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(s/def ::watcher #(satisfies? Watcher %))

(defprotocol Watcher
  (watch [this paths callback])
  (stop [this]))

;; Stub watcher implementation ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defrecord StubWatcher [on-changes]
  Watcher
  (watch [_ _ callback]
    (reset! on-changes callback)))

(defn new-stub-watcher
  []
  (->StubWatcher (atom nil)))

(defn trigger-change-callback
  [stub-watcher abs-path]
  (let [on-changes @(:on-changes stub-watcher)]
    (on-changes abs-path)))

;; Real filesystem watcher implementation ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defrecord HawkWatcher [watcher]
  Watcher
  (watch [_ paths callback]
    (let [w (hawk/watch!
              [{:paths paths
                :handler (fn [_ {:keys [file]}]
                           (callback (.getAbsolutePath ^File file)))}])]
      (reset! watcher w)))
  
  (stop [_]
    (when @watcher
      (hawk/stop! @watcher)
      (reset! watcher nil))))

(defn new-hawk-watcher
  []
  (->HawkWatcher (atom nil)))
