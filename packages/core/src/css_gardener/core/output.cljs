(ns css-gardener.core.output
  (:require [clojure.core.async :refer [chan close! go-loop <!]]
            [clojure.spec.alpha :as s]
            [css-gardener.core.file :as file]
            [css-gardener.core.logging :as logging]
            [css-gardener.core.utils.fs :as fs]
            [integrant.core :as ig]))

(defmethod ig/init-key ::output-channel
  [_ {:keys [buffer] :or {buffer 100}}]
  (chan buffer))

(defmethod ig/halt-key! ::output-channel
  [_ output-channel]
  (close! output-channel))

(defmethod ig/init-key ::consumer
  [_ {:keys [logger output-channel]}]
  (fn []
    (logging/debug logger "Starting output consumer")
    (go-loop []
      (let [value (<! output-channel)]
        (cond
          (nil? value)
          (logging/debug logger "Output channel closed, stopping consumer.")

          (= :s/invalid (s/conform ::file/file value))
          (logging/warning logger (str "Received invalid output file: "
                                       (s/explain-data ::file/file value)))

          :else
          (let [{:keys [absolute-path content]} value]
            (<! (fs/write-file absolute-path content))
            (logging/info logger (str "Wrote output file " absolute-path))
            (recur)))))))
