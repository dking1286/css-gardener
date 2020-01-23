(ns main.core
  (:require [main.components.app.core :refer [app]]
            [reagent.core :as r]))

(defn render
  []
  (r/render [app]
            (.getElementById js/document "app")))

(defn ^:dev/after-load main
  []
  (render))
