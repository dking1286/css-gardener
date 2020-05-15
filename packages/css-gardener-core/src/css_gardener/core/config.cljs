(ns
  ^{:doc "Defines the configuration map for css-gardener."}
  css-gardener.core.config
  (:require [clojure.spec.alpha :as s]
            [clojure.string :as string]
            [css-gardener.core.file :as file]
            [css-gardener.core.utils.errors :as errors]))

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
               :file ::file/file))

(defn matching-rule
  "Gets the rule in the configuration map matching a file."
  [config file]
  (let [matching-rules (->> (:rules config)
                            (filter (fn [[ending _]]
                                      (string/ends-with? (:absolute-path file)
                                                         ending)))
                            vec)]
    (case (count matching-rules)
      0 (errors/not-found (js/Error. (str "No rule found in configuration "
                                          "matching file "
                                          (:absolute-path file))))
      1 (second (first matching-rules))
      (errors/conflict (str "More than 1 rule fould matching file "
                            (:absolute-path file)
                            " : "
                            (string/join "," (map first matching-rules)))))))
