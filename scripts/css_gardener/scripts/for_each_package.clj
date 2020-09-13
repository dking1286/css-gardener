(ns css-gardener.scripts.for-each-package
  (:require [clojure.java.io :as io]
            [clojure.java.shell :refer [sh]]
            [clojure.string :as string]
            [clojure.tools.cli :refer [parse-opts]]
            [css-gardener.scripts.utils :refer [get-packages]]))

(def ^:private cli-options
  [["-s" "--serial" "Whether the task should be run in series for each subpackage"]])

(defn- run-command
  [package command]
  (println (str "Running command \"" command "\" in package " package))
  (let [segments (concat (string/split command #" ")
                         [:dir (io/file package)])
        {:keys [exit out err]} (apply sh segments)]
    (if-not (zero? exit)
      (do
        (println out)
        (println err)
        (throw (ex-info (str "Failed to run command \""
                             command
                             "\" in package"
                             package)
                        {})))
      (println out))))

(defn- run-parallel
  [command]
  (->> (get-packages)
       (pmap (fn [package] (run-command package command)))
       doall)
  (shutdown-agents))

(defn- run-serial
  [command]
  (doseq [package (get-packages)]
    (run-command package command)))

(defn -main
  [& args]
  (let [{:keys [options arguments errors]} (parse-opts args cli-options)
        command (first arguments)]
    (cond
      errors (do (println errors) (System/exit 1))
      (:serial options) (run-serial command)
      :else (run-parallel command))))
