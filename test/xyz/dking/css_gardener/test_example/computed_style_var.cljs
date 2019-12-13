(ns xyz.dking.css-gardener.test-example.computed-style-var
  (:require [xyz.dking.css-gardener.test-example.style-vars :as style-vars]))

(def ^:css-gardener/style style
  (conj style-vars/style
        :.hello {:background-color :red}))
