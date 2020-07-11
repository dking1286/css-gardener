(ns ^{:css-gardener/require ["./styles.scss"]}
  css-gardener.sass-example.components.todo-list.core
  (:require [css-gardener.sass-example.components.todo-item.core]
            [css-gardener.scope.core :refer [scoped-classname infer-scope]]))

(def scoped (partial scoped-classname (infer-scope)))

(defn todo-list
  [& items]
  (apply vector :div {:className (scoped "container")} items))