(ns css-gardener.core.utils.fs
  (:refer-clojure :exclude [exists?])
  (:require ["fs" :as fs]
            [clojure.core.async :refer [go]]
            [css-gardener.core.utils.async :as a]
            [integrant.core :as ig]))

(defn exists?
  "Determine if a file exists, and that the process has read permission."
  [filename]
  (a/node-callback->channel
   fs/access filename fs/constants.R_OK (fn [err] (nil? err))))

(defn read-file
  "Reads the contents of a file as a UTF-8 string"
  [filename]
  (a/node-callback->channel
   fs/readFile filename "utf8" (fn [err contents]
                                 (or err contents))))

(defmethod ig/init-key ::exists?
  [_ {:keys [return-value existing-files]}]
  (cond
    return-value (a/constantly return-value)
    existing-files (fn [file] (go (existing-files file)))
    :else exists?))

(defmethod ig/init-key ::read-file
  [_ {:keys [return-value]}]
  (if return-value
    (fn [& _] (go return-value))
    read-file))
