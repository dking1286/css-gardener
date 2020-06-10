(ns ^:figwheel-hooks css-gardener.scope.core-dev
  (:require [clojure.test :refer [run-tests]]
            [css-gardener.scope.core-test]))

(defn ^:after-load after-load
  "Lifecycle hook for when figwheel reloads code in development."
  []
  (run-tests 'css-gardener.scope.core-test))