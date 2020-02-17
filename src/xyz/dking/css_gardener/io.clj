(ns xyz.dking.css-gardener.io
  (:require [clojure.java.io :as io]
            [clojure.spec.alpha :as s]
            [clojure.string :as str]
            [xyz.dking.css-gardener.utils :as utils])
  (:import [java.io File]))

(declare Reader Writer)

(s/def ::absolute-path
  (s/and string? #(str/starts-with? % "/")))

;; Reader protocol definition ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(s/def ::reader #(satisfies? Reader %))

(defprotocol Reader
  (read-file [this abs-path])
  (get-absolute-path [this relative-path])
  (expand-globs [this globs]))

(s/fdef read-file
  :args (s/cat :this ::reader
               :abs-path ::absolute-path)
  :ret (s/nilable string?))

(s/fdef get-absolute-path
  :args (s/cat :this ::reader
               :relative-path string?)
  :ret ::absolute-path)

(s/fdef expand-globs
  :args (s/cat :this ::reader
               :globs (s/coll-of string?))
  :ret (s/coll-of ::absolute-path))

;; Stub reader implementation ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defrecord StubReader [files paths globs-map]
  Reader
  (read-file [_ abs-path]
    (get @files abs-path))
  
  (get-absolute-path [_ relative-path]
    (get paths relative-path))

  (expand-globs [_ globs]
    (->> globs
         (mapcat #(get globs-map %))
         (utils/unique-by identity)
         sort)))

(defn new-stub-reader
  [files paths globs-map]
  (->StubReader (atom files) paths globs-map))

(s/fdef new-stub-reader
  :args (s/cat :files (s/map-of ::absolute-path string?)
               :paths (s/map-of string? ::absolute-path)
               :globs-map (s/map-of string? (s/coll-of ::absolute-path)))
  :ret ::reader)

(defn update-file!
  "Updates the text of the file that will be returned by the stub reader."
  [stub-reader abs-path new-text]
  (swap! (:files stub-reader) assoc abs-path new-text))

;; Filesystem reader implementation ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defrecord FileReader []
  Reader
  (read-file [_ abs-path]
    (let [file (io/file abs-path)]
      (if-not (.exists file)
        nil
        (slurp file))))
  
  (get-absolute-path [_ relative-path]
    (.getAbsolutePath ^File (io/file relative-path)))

  (expand-globs [_ globs]
    (->> (utils/unique-files globs)
         (map #(.getAbsolutePath ^File %)))))

(defn new-file-reader
  []
  (->FileReader))

(s/fdef new-file-reader
  :ret ::reader)

;; Writer protocol definition ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(s/def ::writer #(satisfies? Writer %))

(defprotocol Writer
  (write-file [this abs-path text]))

(s/fdef write-file
  :args (s/cat :this ::writer
               :abs-path ::absolute-path
               :text string?)
  :ret nil?)

;; Stub writer implementation ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defrecord StubWriter [files]
  Writer
  (write-file [_ abs-path text]
    (swap! files assoc abs-path text)))

(defn new-stub-writer
  []
  (->StubWriter (atom {})))

(s/fdef new-stub-writer
  :ret ::writer)

(defn get-written-file
  [stub-writer path]
  (-> stub-writer :files deref (get path)))

;; Filesystem writer implementation ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defrecord FileWriter []
  Writer
  (write-file [_ abs-path text]
    (let [file (io/file abs-path)]
      (io/make-parents file)
      (spit file text))))

(defn new-file-writer
  []
  (->FileWriter))

(s/fdef new-file-writer
  :ret ::writer)
