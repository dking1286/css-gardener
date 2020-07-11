(ns ^{:css-gardener/require ["./styles.scss"]}
  css-gardener.sass-example.components.todo-item.core
  (:require [css-gardener.scope.core :refer [scoped-classname infer-scope]]))

(def scoped (partial scoped-classname (infer-scope)))

(defn todo-item
  [text]
  [:div {:className (scoped "container")} text])
