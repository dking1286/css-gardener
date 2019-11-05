(ns xyz.dking.css-gardener.analyzer
  (:require [cljs.analyzer.api :as analyzer]
            [cljs.env :as env]))

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
      (analyzer/analyze-file file)
      env/*compiler*)))

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

(defn all-style-vars
  "Gets a seq of namespace-qualified symbols representing
  style vars defined in the passed in files."
  [files]
  (->> files
       (map analyze-file)
       (map get-non-core-ns-map)
       (mapcat ns-style-vars)))

