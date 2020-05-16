(ns ^{:css-gardener/require ["./styles.css"]}
  css-gardener.css-example.components.todo-item.core)

(defn todo-item
  [text]
  [:div.todo-item text])