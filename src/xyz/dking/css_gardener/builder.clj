(ns xyz.dking.css-gardener.builder
  (:require [clojure.spec.alpha :as s]
            [xyz.dking.css-gardener.config :as config]))

(declare Builder)

(s/def ::builder (partial satisfies? Builder))

(s/def ::result string?)
(s/def ::error string?)

(s/def ::output-file
  (s/and :config/file-details
         (s/or :success (s/keys :req-un [::result])
               :failure (s/keys :req-un [::error]))))

(s/fdef start
  :args (s/cat :builder ::builder)
  :ret nil?)

(s/fdef stop
  :args (s/cat :builder ::builder)
  :ret nil?)

(s/fdef build
  :args (s/cat :builder ::builder
               :config :config/augmented-config)
  :ret (s/coll-of ::output-file))

(s/fdef watch
  :args (s/cat :builder ::builder
               :config :config/augmented-config
               :on-change (s/fspec
                           :args (s/cat :output-files (s/coll-of ::output-file))
                           :ret nil?))
  :ret nil?)

(defprotocol Builder
  (start [builder])
  (stop [builder])
  (build [builder config])
  (watch [builder config on-change]))

