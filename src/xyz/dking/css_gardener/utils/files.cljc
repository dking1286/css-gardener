(ns xyz.dking.css-gardener.utils.files
  (:refer-clojure :exclude [exists?])
  (:require #?(:clj [clojure.java.io :as io]
               :cljs ["fs" :as fs])))

(defn exists?
  "Determines if a file exists."
  [filename]
  #?(:clj (.exists (io/file filename))
     :cljs (fs/existsSync filename)))

(defn read-file
  "Reads the contents of a file as a string."
  [filename]
  #?(:clj (slurp filename)
     :cljs (fs/readFileSync filename "utf8")))

