(ns xyz.dking.css-gardener.analyzer
  (:require [clojure.java.io :as io]
            [clojure.string :as string]
            [cljs.analyzer.api :as analyzer]
            [cljs.env :as env]
            [xyz.dking.css-gardener.logging :as logging]
            [xyz.dking.css-gardener.utils :refer [map-vals]]))

(def ^:private core-ns-names
  #{'cljs.core 'cljs.user})

(defn- core-ns?
  "Determines if a cljs namespace name is one of the
  default namespaces included in the compiler state
  by the cljs analyzer."
  [ns-sym]
  (core-ns-names ns-sym))

(defn- analyze-file
  "Analyzes a file using the cljs analyzer.

  Args:
    file: String filename of the file to analyze

  Returns:
    The compiler state after analysis is done."
  [file]
  (analyzer/with-state (analyzer/empty-state)
    (do
      (analyzer/analyze-file (io/file file))
      @env/*compiler*)))

(defn- get-non-core-ns-map
  "Gets the first ns map from the compiler state that is not
  one of the core namespaces."
  [compiler-state]
  (->> compiler-state
       :cljs.analyzer/namespaces
       (filter (fn [[name ns-map]] (not (core-ns? name))))
       vals
       first))

(defn- ns-style-vars
  "Gets a seq of namespace-qualified symbols representing
  style vars defined in a namespace.

  Args:
    ns-map: Map representing a namespace of the form produced by the
      cljs analyzer.

  Returns:
    Seq of symbols"
  [ns-map]
  (->> ns-map
       :defs
       vals
       (filter :css-gardener/style)
       (map :name)))

(defn- exists?
  "Determines whether or not a file exists."
  [filename]
  (.exists (io/file filename)))

(defn- cljs-file?
  "Determines whether or not a filename represents a cljs file."
  [filename]
  (string/ends-with? filename ".cljs"))

(defn- warn-for-non-existent-file
  "Logs a warning if a file does not exist."
  [filename]
  (when (not (exists? filename))
    (logging/warn (str "File " filename
                       " does not exist, skipping.")))
  filename)

(defn- warn-for-non-cljs-file
  "Logs a warning if a file is not a cljs file."
  [filename]
  (when (not (cljs-file? filename))
    (logging/warn (str "File " filename
                       " is not a CLJS file, skipping.")))
  filename)

(defn- warn-for-missing-style-vars
  "Logs a warning if a cljs input file does not have any
  style vars."
  [[filename style-vars]]
  (when (empty? style-vars)
    (logging/warn (str "File " filename
                       " does not contain any style definitions.")))
  [filename style-vars])

(defn all-style-vars
  "Gets a seq of namespace-qualified symbols representing
  style vars defined in the passed in files."
  [files]
  (->> files
       (map warn-for-non-existent-file)
       (filter exists?)
       (map warn-for-non-cljs-file)
       (filter cljs-file?)
       (map (fn [filename] [filename filename]))
       (map-vals analyze-file)
       (map-vals get-non-core-ns-map)
       (map-vals ns-style-vars)
       (map warn-for-missing-style-vars)
       (map second)
       (apply concat)))


