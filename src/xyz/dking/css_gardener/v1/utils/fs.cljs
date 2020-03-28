(ns xyz.dking.css-gardener.v1.utils.fs
  (:refer-clojure :exclude [exists?])
  (:require ["fs" :as fs]
            ["glob" :as node-glob]
            [clojure.core.async :as ca]
            [xyz.dking.css-gardener.v1.utils.async :as a]
            [xyz.dking.css-gardener.v1.utils.seq :refer [unique]]))

(defn glob
  [pattern]
  (a/node-callback->channel
    node-glob pattern (fn [err files] (or err (vec files)))))

(defn unique-files
  "Gets a seq of all the unique files matching a seq of globs"
  [patterns]
  (->> patterns
       (map glob)
       ca/merge
       (ca/reduce concat [])
       (a/map unique)))

(defn exists?
  "Determine if a file exists, and that the process has read permission."
  [filename]
  (a/node-callback->channel
    fs/access filename fs/constants.R_OK (fn [err] (nil? err))))

(defn read-file
  "Reads the contents of a file as a UTF-8 string"
  [filename]
  (a/node-callback->channel
    fs/readFile filename "utf8" (fn [err contents] (or err contents))))
