(ns ^{:css-gardener/require ["./<%= styleName %>"]}
  <%= nsPrefix %>.<%= componentName %>.core
  (:require [css-gardener.scope.core :refer [scoped-classname infer-scope]]))

(def ^:private scoped (memoize (partial scoped-classname (infer-scope))))

(defn <%= componentName %>
  "FIXME write a docstring."
  []
  [:div {:className (scoped "container")}
   "I am component <%= componentName %>"])
