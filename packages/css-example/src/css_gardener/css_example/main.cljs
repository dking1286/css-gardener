(ns css-gardener.css-example.main
  (:require [css-gardener.css-example.components.app.core :refer [app]]
            [reagent.dom :as rd]))

(defn- render
  []
  (rd/render [app]
             (js/document.querySelector "#main")))

(defn ^:dev/after-load after-load
  "Lifecycle hook that is called after new code is loaded in development"
  []
  (render))

(defn main
  "Entry point for the application"
  []
  (render))
