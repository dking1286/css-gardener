(ns css-gardener.scripts.utils
  (:require [clojure.data.json :as json]
            [clojure.data.xml :as xml]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [me.raynes.fs :as fs]))

(defn edn-parse
  "Parses EDN into Clojure data structures."
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

(defn json-serialize
  "Serializes Clojure data structures into JSON"
  [data]
  (json/write-str data))

(defn get-packages
  "Gets a seq of all packages inside the mororepo."
  []
  (->> (.listFiles (io/file "packages"))
       (map #(str "packages/" (.getName %)))
       sort))

(defn remove-dir
  "Synchronously remove a directory and all of its contents."
  [name]
  (fs/delete-dir name))

(defn xml-parse
  "Parses an xml string into Clojure data structures."
  [xml]
  (xml/parse-str xml))

(defn xml-serialize
  "Serializes Clojure data into an xml string."
  [data]
  (xml/indent-str data))