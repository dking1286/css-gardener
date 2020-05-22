(ns css-gardener.core.utils.testing
  (:require-macros [css-gardener.core.utils.testing])
  (:require [clojure.spec.test.alpha :as stest]
            [integrant.core :as ig]))

(def ^{:doc "Fixture that instruments all instrumentable symbols before each
             test case, and then uninstruments them. Does nothing
             if goog.DEBUG is set to false, as it is in release builds."}
  instrument-specs
  {:before #(when (identical? true js/goog.DEBUG)
              (stest/instrument))
   :after #(when (identical? true js/goog.DEBUG)
             (stest/unstrument))})

(defn init-system
  "Initializes an integrant system, printing a more useful error message
   if an error occurs during initialization.
   
   Note: This function is primarily intended for use in the with-system
   macro, defined in testing.clj."
  [sys-config]
  (try
    (ig/init sys-config)
    (catch js/Error err
      (println (ex-cause err))
      (throw err))))