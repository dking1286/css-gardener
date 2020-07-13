(ns ^{:css-gardener/require ["./param-style-name"]}
  param-components-path.param-component-name.core
  (:require [css-gardener.scope.core :refer [scoped-classname infer-scope]]))

(def ^:private scoped (memoize (partial scoped-classname (infer-scope))))

(defn param-component-name
  "FIXME write a docstring."
  []
  [:div {:className (scoped "container")}
   "I am component param-component-name"])
