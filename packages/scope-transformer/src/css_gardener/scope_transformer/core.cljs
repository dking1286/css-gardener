(ns css-gardener.scope-transformer.core
  (:require [css-gardener.common.object :refer [object-merge]]
            [css-gardener.scope.core :refer [scope-from-stylesheet]]
            [goog.object :as object]))

(defn enter
  "Entry function for the scope transformer."
  [file _ callback]
  (let [scope (scope-from-stylesheet (object/get file "content"))
        outfile (if scope
                  (object-merge file #js {:scopeTransformerScope scope})
                  file)]
    (callback nil outfile)))

(defn exit
  "TODO: Add me"
  [file _ callback]
  (callback nil file))