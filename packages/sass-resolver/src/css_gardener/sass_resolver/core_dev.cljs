(ns css-gardener.sass-resolver.core-dev
  (:require [clojure.test :refer [run-tests]]
            [css-gardener.sass-resolver.core]
            [css-gardener.sass-resolver.core-test]))

(defn ^:dev/after-load after-load
  "Lifecycle function that is called after new code is loaded in development"
  []
  (run-tests 'css-gardener.sass-resolver.core-test))

(defn main
  "Stub main function for development"
  [])
