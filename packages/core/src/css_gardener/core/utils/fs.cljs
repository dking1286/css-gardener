(ns css-gardener.core.utils.fs
  (:refer-clojure :exclude [exists?])
  (:require [clojure.core.async :refer [go]]
            [css-gardener.core.utils.async :as a]
            [css-gardener.core.utils.errors :as errors]
            [fs]
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

(defn write-file
  "Writes text to a file."
  [filename data]
  (a/node-callback->channel
   fs/writeFile filename data (fn [err] err)))

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
                  (errors/not-found (str filename " not found")))))
    ;; Real read-file implementation
    :else read-file))

(defmethod ig/init-key ::write-file
  [_ {:keys [;; Atom of a vector. If provided, create a stub implementation
             ;; that appends the files to the atom instead of writing to the
             ;; filesystem.
             written-files]}]
  (if written-files
    (fn [filename content]
      (swap! written-files conj {:absolute-path filename
                                 :content content}))
    write-file))
