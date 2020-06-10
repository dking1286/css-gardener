(ns css-gardener.scope.test-runner
  (:require [clojure.test :refer [run-tests]]
            [css-gardener.scope.core-test]))

(run-tests 'css-gardener.scope.core-test)