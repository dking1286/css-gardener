(ns css-gardener.scripts.for-each-package
  (:require [clojure.java.io :as io]
            [clojure.java.shell :refer [sh]]
            [clojure.string :as string]
            [css-gardener.scripts.utils :refer [get-packages]]))

(defn -main
  [command]
  (->> (get-packages)
       (pmap (fn [package]
               (println (str "Running command \""
                             command
                             "\" in package "
                             package))
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
                   (println out)))))
       doall)
  (shutdown-agents))
