(ns xyz.dking.css-gardener.io
  (:require [clojure.spec.alpha :as s]
            [clojure.string :as str]
            [clojure.java.io :as io]))

(declare Reader Writer)

(s/def ::absolute-path
  (s/and string? #(str/starts-with? % "/")))

(s/def ::reader #(satisfies? Reader %))

(s/fdef read-file
  :args (s/cat :this ::reader
               :abs-path ::absolute-path)
  :ret (s/nilable string?))

(defprotocol Reader
  (read-file [this abs-path]))

(defrecord StubReader [files]
  Reader
  (read-file [this abs-path]
    (get files abs-path)))

(s/fdef new-stub-reader
  :args (s/cat :files (s/map-of ::absolute-path string?))
  :ret ::reader)

(defn new-stub-reader
  [files]
  (->StubReader files))

(defrecord FileReader []
  Reader
  (read-file [this abs-path]
    (let [file (io/file abs-path)]
      (if-not (.exists file)
        nil
        (slurp file)))))

(s/fdef new-file-reader
  :ret ::reader)

(defn new-file-reader
  []
  (->FileReader))

(s/def ::writer #(satisfies? Writer %))

(s/fdef write-file
  :args (s/cat :this ::writer
               :abs-path ::absolute-path
               :text string?)
  :ret nil?)

(defprotocol Writer
  (write-file [this abs-path text]))

(defrecord StubWriter [files]
  Writer
  (write-file [this abs-path text]
    (swap! files assoc abs-path text)))

(s/fdef new-stub-writer
  :ret ::writer)

(defn new-stub-writer
  []
  (->StubWriter (atom {})))

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


