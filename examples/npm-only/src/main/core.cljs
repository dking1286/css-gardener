(ns main.core
  (:require [reagent.core :as r]
            [components.app.core :refer [app]]))

(defn render
  []
  (r/render [app]
            (js/document.getElementById "app")))

(defn ^:dev/after-load main
  []
  (render))

