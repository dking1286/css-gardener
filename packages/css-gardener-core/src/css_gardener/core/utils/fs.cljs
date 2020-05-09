(ns css-gardener.core.utils.fs
  (:refer-clojure :exclude [exists?])
  (:require ["fs" :as fs]
            [clojure.core.async :refer [go]]
            [css-gardener.core.utils.async :as a]
            [css-gardener.core.utils.errors :as errors]
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
  [_ {:keys [return-value files]}]
  (cond
    ;; Mock exists? with hard-coded return value
    return-value (fn [_] (go return-value))
    ;; Mock exists? with hard-coded set of existing files
    files (fn [file] (go (contains? files file)))
    ;; Real exists? implementation
    :else exists?))

(defmethod ig/init-key ::read-file
  [_ {:keys [return-value files]}]
  (cond
    ;; Mock read-file with hard-coded return value
    return-value (fn [_] (go return-value))
    ;; Mock read-file with hard-coded map of filenames to file content
    files (fn [filename]
            (go
              (or (get files filename)
                  (errors/not-found (js/Error. (str filename " not found"))))))
    ;; Real read-file implementation
    :else read-file))
