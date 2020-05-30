(ns css-gardener.sass-resolver.core)

(defn main
  "Dependency resolver for scss and sass stylesheets."
  [_ _ ^js/Function callback]
  (callback nil #js []))