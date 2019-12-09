(ns xyz.dking.css-gardener.config
  (:require [clojure.spec.alpha :as s]))

(s/def ::type keyword?)
(s/def ::input-files (s/coll-of string? :kind vector? :min-count 1))
(s/def ::output-file string?)

(s/def ::config (s/keys :req-un [::type
                                 ::input-files
                                 ::output-file]))


