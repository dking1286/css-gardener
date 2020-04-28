(ns css-gardener.core.utils.testing
  (:require [clojure.spec.test.alpha :as stest]))

(def instrument-specs {:before #(stest/instrument)
                       :after #(stest/unstrument)})