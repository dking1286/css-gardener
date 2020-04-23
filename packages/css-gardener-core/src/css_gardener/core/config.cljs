(ns ^{:doc "Defines the configuration map for css-gardener."}
 css-gardener.core.config
  (:require [clojure.spec.alpha :as s]))

(s/def ::infer-source-paths-and-builds string?)

(s/def ::source-paths (s/coll-of string? :kind vector?))

(s/def ::entries (s/coll-of symbol? :kind vector?))
(s/def ::depends-on (s/coll-of keyword? :kind set?))
(s/def ::module (s/keys :req-un [::entries] :opt-un [::depends-on]))
(s/def ::modules (s/map-of keyword? ::module))
(s/def ::build (s/keys :req-un [::modules]))
(s/def ::builds (s/map-of keyword? ::build))

(s/def ::node-module string?)
(s/def ::options map?)
(s/def ::transformer (s/keys :req-un [::node-module] :opt-un [::options]))
(s/def ::transformers (s/coll-of ::transformer :kind vector?))
(s/def ::rule (s/keys :req-un [::transformers]))
(s/def ::rules (s/map-of regexp? ::rule))

(s/def ::config
  (s/and
   (s/keys :req-un [::rules])
   (s/or :explicit (s/keys :req-un [::source-paths ::builds])
         :inferred (s/keys :req-un [::infer-source-paths-and-builds]))))
