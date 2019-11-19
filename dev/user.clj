(ns user
  (:require [clojure.tools.namespace.repl :as ctnr]))

(defn reset
  []
  (ctnr/refresh))
