(ns xyz.dking.css-gardener.main
  (:require [xyz.dking.css-gardener.builder :as builder]
            [xyz.dking.css-gardener.config :as config]
            [xyz.dking.css-gardener.core :refer [build watch]]))

(def help-message
  "TODO")

(defn- missing-default-config-file?
  "Determines if the default config file is missing."
  [status config-file]
  (and (= status :failure)
       (= config-file config/default-config-file)))

(defn- missing-custom-config-file?
  "Determines if the custom config file is missing."
  [status config-file]
  (and (= status :failure)
       (not= config-file config/default-config-file)))

(defn main
  "The main process of css-gardener."
  [command & args]
  (let [{:keys [help watch config-file] :as cli-config}
        (config/from-cli-args args)

        {status :status reason :reason file-config :result}
        (config/from-file config-file)

        config
        (merge file-config cli-config)]
    (if (or (= command "--help") (= command "-h"))
      (println help-message)
      (do
        (when (missing-default-config-file? status config-file)
          (println (str "Default configuration file " config/default-config-file
                        " not found, using command line args.")))
        (when (missing-custom-config-file? status config-file)
          (println (str "WARNING: Configuration file " config-file
                        " not found, using only command line args.")))
        
        (case command
          "watch" (watch config)
          "build" (build config)
          (throw (ex-info (str "Unknown command \"" command "\".")
                          {:type :unknown-command})))))))

(defn -main
  [& args]
  (apply main args))
