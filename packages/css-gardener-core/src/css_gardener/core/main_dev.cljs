(ns css-gardener.core.main-dev
  (:require [clojure.spec.test.alpha :as stest]
            [css-gardener.core.main :as main]))

(defn ^:dev/before-load before-load
  []
  (stest/unstrument)
  (println "Reloading..."))

(defn ^:dev/after-load after-load
  []
  (stest/instrument)
  (println "done!"))

(defn main
  [& _]
  (stest/instrument)
  (main/main))