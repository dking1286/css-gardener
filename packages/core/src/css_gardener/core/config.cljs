(ns
  ^{:doc "Defines the configuration map for css-gardener."}
  css-gardener.core.config
  (:require [clojure.spec.alpha :as s]
            [clojure.string :as string]
            [css-gardener.core.utils.errors :as errors]
            [integrant.core :as ig]))

(s/def ::infer-source-paths-and-builds string?)

(s/def ::source-paths (s/coll-of string? :kind vector?))

(s/def ::entries (s/coll-of symbol? :kind vector?))
(s/def ::depends-on (s/coll-of keyword? :kind set?))
(s/def ::module (s/keys :req-un [::entries] :opt-un [::depends-on]))
(s/def ::modules (s/map-of keyword? ::module))
(s/def ::output-dir string?)
(s/def ::build (s/keys :req-un [::modules ::output-dir]))
(s/def ::builds (s/map-of keyword? ::build))

(s/def ::node-module string?)
(s/def ::options map?)
(s/def ::dependency-resolver (s/keys :req-un [::node-module]))
(s/def ::transformer (s/keys :req-un [::node-module] :opt-un [::options]))
(s/def ::transformers (s/coll-of ::transformer :kind vector?))
(s/def ::rule (s/keys :req-un [::transformers]))
(s/def ::rules (s/map-of string? ::rule))

(s/def ::config
  (s/and
   (s/keys :req-un [::rules])
   (s/or :explicit (s/keys :req-un [::source-paths ::builds])
         :inferred (s/keys :req-un [::infer-source-paths-and-builds]))))

(s/fdef matching-rule
  :args (s/cat :config ::config
               :absolute-path string?))

(defn matching-rule
  "Gets the rule in the configuration map matching a file."
  [config absolute-path]
  (let [matching-rules (->> (:rules config)
                            (filter (fn [[ending _]]
                                      (string/ends-with? absolute-path
                                                         ending)))
                            vec)]
    (case (count matching-rules)
      0 (errors/not-found (str "No rule found in configuration "
                               "matching file "
                               absolute-path))
      1 (second (first matching-rules))
      (errors/conflict (str "More than 1 rule fould matching file "
                            absolute-path
                            " : "
                            (string/join "," (map first matching-rules)))))))

(defmethod ig/init-key ::config
  [_ config]
  (let [conformed (s/conform ::config config)]
    (when (= ::s/invalid conformed)
      (throw (errors/invalid-config (str "System configuration key "
                                         ::config
                                         " was not a valid configuration map. "
                                         (s/explain-data ::config config)))))
    (when (= :inferred (first conformed))
      (throw (errors/invalid-config (str "System configuration key "
                                         ::config
                                         " must have explicit source-paths "
                                         "and builds."))))
    (second conformed)))

(comment
  (s/conform ::config {:source-paths []
                       :builds {}
                       :rules {}})
  (s/conform ::config {:blah :blah}))