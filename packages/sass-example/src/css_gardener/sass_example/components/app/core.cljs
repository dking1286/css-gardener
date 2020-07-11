(ns css-gardener.sass-example.components.app.core
  (:require [css-gardener.sass-example.components.todo-item.core :refer [todo-item]]
            [css-gardener.sass-example.components.todo-list.core :refer [todo-list]]))

(defn app
  []
  [:div.app
   [todo-list
    [todo-item "Thing 1"]
    [todo-item "Thing 2"]
    [todo-item "Thing 3"]]])