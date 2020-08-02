(ns css-gardener.scripts.utils
  (:refer-clojure :exclude [exists?])
  (:require [cljs.reader :as reader]
            [fs-extra]))

(defn exists?
  "Determines whether or not a file exists."
  [filename]
  (fs-extra/existsSync filename))

(defn slurp
  "Synchronously reads a file."
  [filename]
  (fs-extra/readFileSync filename "utf8"))

(defn spit
  "Synchronously writes a file."
  [filename content]
  (fs-extra/writeFileSync filename content))

(defn remove-dir
  "Synchronously remove a directory and all of its contents."
  [name]
  (fs-extra/removeSync name))

(defn json-parse
  "Parses JSON into cljs data structures."
  [json]
  (js->clj (js/JSON.parse json)))

(defn edn-parse
  "Parses EDN into cljs data structures."
  [edn]
  (reader/read-string edn))

(defn get-packages
  "Gets a list of all packages inside the monorepo."
  []
  (-> (slurp "lerna.json")
      json-parse
      (get "packages")))