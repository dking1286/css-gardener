(ns css-gardener.core.change-detection
  (:require [chokidar]
            [clojure.core.async :refer [chan close! go-loop <! put!]]
            [css-gardener.core.logging :as logging]
            [integrant.core :as ig]))

(defmethod ig/init-key ::input-channel
  [_ _]
  (chan 100))

(defmethod ig/halt-key! ::input-channel
  [_ input-channel]
  (close! input-channel))

(def ^:private dotfiles-regexp #"(^|[\/\\])\..")

(defmethod ig/init-key ::watcher
  [_ {:keys [logger source-paths input-channel]}]
  (when (seq source-paths)
    (logging/debug logger "Starting file watcher")
    (let [watcher (-> (chokidar/watch (clj->js source-paths)
                                      #js {:ignored dotfiles-regexp
                                           :ignoreInitial true})
                      (.on "add"
                           #(put! input-channel {:type :add :path %}))
                      (.on "change"
                           #(put! input-channel {:type :change :path %}))
                      (.on "unlink"
                           #(put! input-channel {:type :unlink :path %})))]
      {:watcher watcher :logger logger})))

(defmethod ig/halt-key! ::watcher
  [_ {:keys [watcher logger]}]
  (when watcher
    (logging/debug logger "Stopping file watcher")
    (.close watcher)))

(defmethod ig/init-key ::consumer
  [_ {:keys [logger input-channel]}]
  (fn []
    (logging/debug logger "Starting change consumer")
    (go-loop []
      (let [value (<! input-channel)]
        (if value
          (do
            (logging/debug logger (str "Detected changes: " value))
            (recur))
          (logging/debug logger
                         "Input channel closed, stopping change consumer"))))))