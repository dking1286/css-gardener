(ns main.components.app.core
  (:require [main.components.todo-list.core :refer [todo-list]]))

(defn app
  []
  [:div.app
   [todo-list "First" "Second" "Third"]])
