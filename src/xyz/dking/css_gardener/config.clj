(ns xyz.dking.css-gardener.config
  (:require [clojure.edn :as edn]
            [clojure.spec.alpha :as s]
            [clojure.string :as string]
            [clojure.tools.cli :as cli])
  (:import [java.io FileNotFoundException]))

(s/def ::type keyword?)
(s/def ::input-files (s/coll-of string? :kind vector? :min-count 1))
(s/def ::output-file string?)

(s/def ::config (s/keys :req-un [::type ::input-files ::output-file]))

(def default-config-file "css-gardener.edn")

(defn from-file
  "Gets a configuration map from a configuration file."
  [filename]
  (try
    {:status :success
     :result (edn/read-string (slurp filename))}
    (catch FileNotFoundException e
      {:status :failure
       :reason :config-file-not-found
       :error e})
    (catch RuntimeException e
      {:status :failure
       :reason :config-file-invalid
       :error e})))

(defn- parse-input-files
  [input-files]
  (->> (string/split input-files #",")
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

   ["-w" "--watch"
    "Whether or not to watch and rebuild on change"]

   ["-h" "--help"]])

(defn from-cli-args
  "Parses command line args into a configuration map."
  [args]
  (:options (cli/parse-opts args cli-options)))
