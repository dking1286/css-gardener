(ns xyz.dking.css-gardener.utils.fs
  (:refer-clojure :exclude [exists?])
  (:require ["fs" :as fs]
            [clojure.core.async :refer [go >! chan]]
            [clojure.string :as string]
            [xyz.dking.css-gardener.utils.async :refer [callback->channel
                                                        node-callback->channel]]
            [xyz.dking.css-gardener.utils.errors :as errors]))

(defn exists?
  "Determine if a file exists, and that the process has read permission."
  [filename]
  (node-callback->channel
    fs/access filename fs/constants.R_OK (fn [err] (nil? err))))

(defn read-file
  "Reads the contents of a file as a UTF-8 string"
  [filename]
  (node-callback->channel fs/readFile filename "utf8"
                          (fn [err contents] (or err contents))))