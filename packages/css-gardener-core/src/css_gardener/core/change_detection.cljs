(ns css-gardener.core.change-detection
  (:require ["chokidar" :as chokidar]
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
  [_ {:keys [source-paths input-channel]}]
  (when (seq source-paths)
    (-> (chokidar/watch (clj->js source-paths) #js {:ignored dotfiles-regexp})
        (.on "add" #(put! input-channel {:type :add :path %}))
        (.on "change" #(put! input-channel {:type :change :path %}))
        (.on "unlink" #(put! input-channel {:type :unlink :path %})))))

(defmethod ig/halt-key! ::watcher
  [_ watcher]
  (when watcher
    (.close watcher)))

(defmethod ig/init-key ::consumer
  [_ {:keys [logger input-channel]}]
  (fn []
    (logging/debug logger "Starting consumer")
    (go-loop []
      (let [value (<! input-channel)]
        (if value
          (do
            (logging/info logger (str "Detected changes to " value))
            (recur))
          (logging/debug logger
                         "Input channel closed, stopping consumer"))))))