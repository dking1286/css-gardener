(ns css-gardener.core.utils.fs
  (:refer-clojure :exclude [exists?])
  (:require ["fs" :as fs]
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

(defmethod ig/init-key :fs
  [_ _]
  {:exists? exists?
   :read-file read-file})