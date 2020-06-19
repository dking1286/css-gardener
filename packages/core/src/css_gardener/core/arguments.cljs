(ns css-gardener.core.arguments
  (:require [clojure.edn :as edn]
            [clojure.spec.alpha :as s]
            [clojure.string :as string]
            [clojure.tools.cli :refer [parse-opts]]
            [css-gardener.core.config :as config]
            [css-gardener.core.logging :as logging]))

(def ^:private commands #{:watch :compile :release})

(s/def ::command commands)

(def ^:private cli-options
  [["-h" "--help"]
   ["-l" "--log-level LEVEL" "Log level, defaults to 'info' if not provided"
    :default "info"
    :default-fn (constantly :info)
    :parse-fn keyword
    :validate [#(s/valid? ::logging/level %)
               (str "Log level must be one of "
                    (->> (keys logging/goog-level)
                         (map name)
                         (string/join ", ")))]]
   ["-c" "--config-file FILE" "Config file, defaults to 'css-gardener.edn'"
    :default "css-gardener.edn"]
   ["-C" "--config MAP" "Literal configuration map"
    :parse-fn edn/read-string
    :validate [#(s/valid? (s/nilable ::config/config) %)
               (str "Invalid configuration map")]]])

(defn- validate-arguments
  [{[command build-id] :arguments
    :as parsed}]
  (cond
    (not command)
    (update parsed :errors #(vec (conj % "No command given")))

    (not (s/valid? ::command command))
    (update parsed :errors #(vec (conj % (str "Invalid command " command
                                              ", expected one of "
                                              commands))))

    (not build-id)
    (update parsed :errors #(vec (conj % "Missing build id")))

    :else parsed))

(defn parse
  "Parses the command line arguments. Returns a map of the form:
   
   :arguments [...keywords of positional arguments]
   :options {...map of flags}
   :errors [...string error messages]
   :summary string describing command line options"
  [args]
  (-> (parse-opts args cli-options)
      (update :arguments #(map keyword %))
      validate-arguments))
