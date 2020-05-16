(ns ^{:css-gardener/require ["./styles.css"]}
  css-gardener.css-example.components.todo-list.core
  (:require [css-gardener.css-example.components.todo-item.core]))

(defn todo-list
  [& items]
  [:div.todo-list
   items])