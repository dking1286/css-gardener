(ns css-gardener.core.utils.testing
  (:require-macros [css-gardener.core.utils.testing])
  (:require [clojure.spec.test.alpha :as stest]))

(def instrument-specs {:before #(when (identical? true js/goog.DEBUG)
                                  (stest/instrument))
                       :after #(when (identical? false js/goog.DEBUG)
                                 (stest/unstrument))})
