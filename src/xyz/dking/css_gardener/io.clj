(ns xyz.dking.css-gardener.io
  (:require [clojure.java.io :as io]
            [clojure.spec.alpha :as s]
            [clojure.string :as str]
            [xyz.dking.css-gardener.utils :as utils]))

(declare Reader Writer)

(s/def ::absolute-path
  (s/and string? #(str/starts-with? % "/")))

;; Reader protocol definition ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(s/def ::reader #(satisfies? Reader %))

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

(defprotocol Reader
  (read-file [this abs-path])
  (get-absolute-path [this relative-path])
  (expand-globs [this globs]))

;; Stub reader implementation ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defrecord StubReader [files paths globs-map]
  Reader
  (read-file [this abs-path]
    (get @files abs-path))
  
  (get-absolute-path [this relative-path]
    (get paths relative-path))

  (expand-globs [this globs]
    (->> globs
         (mapcat #(get globs-map %))
         (utils/unique-by identity)
         sort)))

(s/fdef new-stub-reader
  :args (s/cat :files (s/map-of ::absolute-path string?)
               :paths (s/map-of string? ::absolute-path)
               :globs-map (s/map-of string? (s/coll-of ::absolute-path)))
  :ret ::reader)

(defn new-stub-reader
  [files paths globs-map]
  (->StubReader (atom files) paths globs-map))

(defn update-file!
  "Updates the text of the file that will be returned by the stub reader."
  [stub-reader abs-path new-text]
  (swap! (:files stub-reader) assoc abs-path new-text))

;; Filesystem reader implementation ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defrecord FileReader []
  Reader
  (read-file [this abs-path]
    (let [file (io/file abs-path)]
      (if-not (.exists file)
        nil
        (slurp file))))
  
  (get-absolute-path [this relative-path]
    (.getAbsolutePath (io/file relative-path)))

  (expand-globs [this globs]
    (->> (utils/unique-files globs)
         (map #(.getAbsolutePath %)))))

(s/fdef new-file-reader
  :ret ::reader)

(defn new-file-reader
  []
  (->FileReader))

;; Writer protocol definition ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(s/def ::writer #(satisfies? Writer %))

(s/fdef write-file
  :args (s/cat :this ::writer
               :abs-path ::absolute-path
               :text string?)
  :ret nil?)

(defprotocol Writer
  (write-file [this abs-path text]))

;; Stub writer implementation ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defrecord StubWriter [files]
  Writer
  (write-file [this abs-path text]
    (swap! files assoc abs-path text)))

(s/fdef new-stub-writer
  :ret ::writer)

(defn new-stub-writer
  []
  (->StubWriter (atom {})))

(defn get-written-file
  [stub-writer path]
  (-> stub-writer :files deref (get path)))

;; Filesystem writer implementation ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defrecord FileWriter []
  Writer
  (write-file [this abs-path text]
    (let [file (io/file abs-path)]
      (io/make-parents file)
      (spit file text))))

(s/fdef new-file-writer
  :ret ::writer)

(defn new-file-writer
  []
  (->FileWriter))


