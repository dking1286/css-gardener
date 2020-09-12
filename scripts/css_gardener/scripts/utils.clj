(ns css-gardener.scripts.utils
  (:require [clojure.data.json :as json]
            [clojure.edn :as edn]
            [clojure.java.io :as io]))

(defn edn-parse
  "Parses EDN into cljs data structures."
  [edn]
  (edn/read-string edn))

(defn exists?
  "Determines whether or not a file exists."
  [filename]
  (.exists (io/file filename)))

(defn json-parse
  "Parses JSON into cljs data structures."
  [json]
  (json/read-str json))

(defn get-packages
  "Gets a seq of all packages inside the mororepo."
  []
  (->> (.listFiles (io/file "packages"))
       (map #(str "packages/" (.getName %)))
       sort))