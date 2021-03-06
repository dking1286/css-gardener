(ns css-gardener.core.output
  (:require [clojure.core.async :refer [chan close! go]]
            [clojure.spec.alpha :as s]
            [css-gardener.core.file :as file]
            [css-gardener.core.logging :as logging]
            [css-gardener.core.utils.async :as a]
            [css-gardener.core.utils.errors :as errors]
            [css-gardener.core.utils.fs :as fs]
            [integrant.core :as ig]))

(defmethod ig/init-key ::output-channel
  [_ {:keys [buffer] :or {buffer 100}}]
  (chan buffer))

(defmethod ig/halt-key! ::output-channel
  [_ output-channel]
  (close! output-channel))

(defn write-output
  "Writes an output file. Logs a warning and skips the file if it is not valid."
  [logger file]
  (if (= :s/invalid (s/conform ::file/file file))
    (let [message (str "Received invalid output file: "
                       (s/explain-data ::file/file file))]
      (logging/warning logger message)
      (go (errors/invalid-argument message)))
    (let [{:keys [absolute-path content]} file]

      (->> (fs/make-parents absolute-path)
           (a/flat-map #(fs/write-file absolute-path content))
           (a/map (fn [result]
                    (when-not (errors/error? result)
                      (logging/info logger
                                    (str "Wrote output file " absolute-path)))
                    result))))))
