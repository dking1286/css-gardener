(ns xyz.dking.css-gardener.builder
  (:require [clojure.spec.alpha :as s]
            [xyz.dking.css-gardener.config :as config]))

(declare Builder)

(s/def ::builder #(satisfies? Builder %))

(s/def ::result string?)
(s/def ::error #(instance? java.lang.Exception %))

(s/def ::output-file
  (s/and ::config/file-details
         (s/or :success (s/keys :req-un [::result])
               :failure (s/keys :req-un [::error]))))

(s/fdef build-file
  :args (s/cat :builder ::builder
               :absolute-path :config/file-details)
  :ret ::output-file)

(defprotocol Builder
  (build-file [builder file-details]))

(defrecord StubBuilder [output-prefix error?]
  Builder
  (build-file [this file-details]
    (if error?
      (assoc file-details
             :error (Exception. (str output-prefix ": " (:text file-details))))
      (assoc file-details
             :result (str output-prefix ": " (:text file-details))))))

(defn new-stub-builder
  [{:keys [output-prefix error?]}]
  (->StubBuilder output-prefix error?))
