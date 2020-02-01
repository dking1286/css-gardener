(ns xyz.dking.css-gardener.main
  (:require [xyz.dking.css-gardener.builder.garden :as garden]
            [xyz.dking.css-gardener.builder.sass :as sass]
            [xyz.dking.css-gardener.config :as config]
            [xyz.dking.css-gardener.core :as core]
            [xyz.dking.css-gardener.logging :as logging]
            [xyz.dking.css-gardener.watcher :as watcher]
            [xyz.dking.css-gardener.io :as gio]))

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

(defn- get-builder
  "Gets an instance of the builder type specified in the config map."
  [config]
  (case (:type config)
    :garden (garden/new-builder config)
    :scss (sass/new-builder config)
    (throw (ex-info
            (str "Unknown :type property " (:type config) " found in config.")
            {:type :unknown-builder-type}))))

(defn main
  "The main process of css-gardener."
  [command & args]
  (let [{:keys [config-file] :as cli-config} (config/from-cli-args args)
        {:keys [status reason result]} (config/from-file config-file)
        config (merge result cli-config)
        log-level (:log-level config)]
    (reset! logging/log-level log-level)
    (cond
      (or (= command "--help") (= command "-h")) (println help-message)
      (= command "init") (core/init config)
      :else
      (do
        (when (missing-default-config-file? status config-file)
          (logging/info (str "Default configuration file "
                             config/default-config-file
                             " not found, using command line args.")))
        (when (missing-custom-config-file? status config-file)
          (logging/info (str "WARNING: Configuration file " config-file
                             " not found, using only command line args.")))
        (let [builder (get-builder config)
              watcher (watcher/hawk-watcher)
              reader (gio/new-file-reader)
              done? (promise)]
          (case command
            "watch" (core/watch builder watcher reader done? config)
            "build" (core/build builder reader config)
            (throw (ex-info (str "Unknown command \"" command "\".")
                            {:type :unknown-command}))))))))

(defn -main
  [& args]
  (apply main args)
  (System/exit 0))
