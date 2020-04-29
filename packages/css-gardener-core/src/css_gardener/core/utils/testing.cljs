(ns css-gardener.core.utils.testing
  (:require-macros [css-gardener.core.utils.testing :refer [with-system]])
  (:require [clojure.spec.test.alpha :as stest]))

(def instrument-specs {:before #(stest/instrument)
                       :after #(stest/unstrument)})
