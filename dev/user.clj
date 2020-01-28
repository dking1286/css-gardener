(ns user
  (:require [clojure.tools.namespace.repl :as ctnr]
            [orchestra.spec.test :as orchestra]))

(defn setup
  []
  (orchestra/instrument))

(defn teardown
  []
  (orchestra/unstrument))

(defn reset
  []
  (teardown)
  (ctnr/refresh :after `setup))
