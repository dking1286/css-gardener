(ns xyz.dking.css-gardener.v1.args
  (:require [clojure.edn :as edn]
            [clojure.string :as s]
            [clojure.tools.cli :refer [parse-opts]]
            [xyz.dking.css-gardener.v1.utils.fs :as fs]
            [xyz.dking.css-gardener.v1.utils.async :as a]))

(def default-config-file "css-gardener.edn")

(defn from-file
  "Gets a configuration map from a configuration file."
  [filename]
  (->> (fs/read-file filename)
       (a/map edn/read-string)))

(defn- parse-input-files
  [input-files]
  (->> (s/split input-files #",")
       (filter (complement empty?))
       (into [])))

(def ^:private cli-options
  [["-t" "--type TYPE"
    "Type of stylesheet"
    :parse-fn keyword]

   ["-i" "--input-files LIST OF INPUT FILES"
    "Comma-separated list of input files"
    :parse-fn parse-input-files]

   ["-o" "--output-file OUTPUT_FILE"
    "Output file name"]

   ["-c" "--config-file CONFIG_FILE"
    "Configuration file name"
    :default default-config-file]

   ["-l" "--log-level LEVEL"
    "Log level"
    :parse-fn keyword
    :default :info]])

(defn from-cli-args
  "Parses command line args into a configuration map."
  [args]
  (:options (parse-opts args cli-options)))

(comment
  (edn/read-string "{hello}")
  (a/trace (from-file "i-dont-exist"))
  (from-cli-args ["-c" "hello" "--log-level" "debug"]))