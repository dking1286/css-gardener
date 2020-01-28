(ns main.components.todo-list.core
  (:require [main.components.todo-list-item.core :refer [todo-list-item]]))

(defn todo-list
  [& items]
  [:div.todo-list
   "This is a list"
   [:ul (for [item items]
          ^{:key item} [todo-list-item item])]])
