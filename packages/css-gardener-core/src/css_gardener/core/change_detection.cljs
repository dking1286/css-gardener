(ns css-gardener.core.change-detection
  (:require ["chokidar" :as chokidar]
            [clojure.core.async :refer [chan close! put!]]
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