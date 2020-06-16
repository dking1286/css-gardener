(ns css-gardener.scope-transformer.core-dev
  (:require [clojure.test :refer [run-tests]]
            [css-gardener.scope-transformer.core-test]))

(defn ^:dev/after-load after-load
  "Lifecycle hook that is called when code is loaded in development."
  []
  (println "Reloaded!")
  (run-tests 'css-gardener.scope-transformer.core-test))

(defn enter
  "TODO: Add me"
  [file _ callback]
  (callback nil file))

(defn exit
  "TODO: Add me"
  [file _ callback]
  (callback nil file))