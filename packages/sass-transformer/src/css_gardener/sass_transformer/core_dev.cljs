(ns css-gardener.sass-transformer.core-dev
  (:require [clojure.test :refer [run-tests]]
            [css-gardener.sass-transformer.core]
            [css-gardener.sass-transformer.core-test]))

(defn ^:dev/after-load after-load
  "Lifecycle function that is called after new code is loaded."
  []
  (run-tests 'css-gardener.sass-transformer.core-test))

(defn enter
  "Stub enter function for the development build."
  [_ _ _])

(defn exit
  "Stub exit function for the development build."
  [_ _ _])