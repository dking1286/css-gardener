(ns css-gardener.core.utils.testing
  (:require-macros [css-gardener.core.utils.testing])
  (:require [clojure.spec.test.alpha :as stest]))

(def ^{:doc "Fixture that instruments all instrumentable symbols before each
             test case, and then uninstruments them. Does nothing
             if goog.DEBUG is set to false, as it is in release builds."}
  instrument-specs
  {:before #(when (identical? true js/goog.DEBUG)
              (stest/instrument))
   :after #(when (identical? true js/goog.DEBUG)
             (stest/unstrument))})
