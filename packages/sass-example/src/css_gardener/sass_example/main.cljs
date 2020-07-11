(ns css-gardener.sass-example.main
  (:require [css-gardener.sass-example.components.app.core :refer [app]]
            [reagent.dom :as rd]))

(defn- render
  []
  (rd/render [app]
             (js/document.querySelector "#main")))

(defn ^:dev/after-load after-load
  []
  (render))

(defn main
  "Entry point for the application"
  []
  (render))
