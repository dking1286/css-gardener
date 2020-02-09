(ns xyz.dking.css-gardener.builder
  (:require [clojure.spec.alpha :as s]
            [clojure.tools.namespace.dependency :as dependency]
            [xyz.dking.css-gardener.config :as config]
            [xyz.dking.css-gardener.io :as gio]))

(declare Builder)

;; Builder protocol ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(s/def ::builder #(satisfies? Builder %))

(s/def ::result string?)
(s/def ::error #(instance? java.lang.Exception %))

(s/def ::output-file
  (s/and ::config/file-details
         (s/or :success (s/keys :req-un [::result])
               :failure (s/keys :req-un [::error]))))

(s/fdef build-file
  :args (s/cat :builder ::builder
               :file-details ::config/file-details)
  :ret ::output-file)

(s/fdef get-dependencies
  :args (s/cat :builder ::builder
               :file-details ::config/file-details)
  :ret (s/coll-of ::gio/absolute-path))

(defprotocol Builder
  (build-file [builder file-details])
  (get-dependencies [builder file-details]))

;; Stub Builder implementation ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defrecord StubBuilder [output-prefix dependencies error?]
  Builder
  (build-file [this file-details]
    (if error?
      (assoc file-details
             :error (Exception. (str output-prefix ": " (:text file-details))))
      (assoc file-details
             :result (str output-prefix ": " (:text file-details)))))

  (get-dependencies [this file-details]
    (get dependencies (:file file-details))))

(defn new-stub-builder
  [{:keys [output-prefix dependencies error?]}]
  (->StubBuilder output-prefix dependencies error?))

;; Other builder functions ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(s/def ::dependency-graph #(satisfies? dependency/DependencyGraph %))

(defn- get-dependency-graph-nodes
  [builder file]
  (->> (get-dependencies builder file)
       (map (fn [dep] [(:file file) dep]))))

(s/fdef get-dependency-graph
  :args (s/cat :builder ::builder
               :files (s/coll-of ::config/file-details))
  :ret ::dependency-graph)

(defn get-dependency-graph
  [builder files]
  (->> files
       (mapcat #(get-dependency-graph-nodes builder %))
       (reduce (fn [graph [file dep]] (dependency/depend graph file dep))
               (dependency/graph))))
