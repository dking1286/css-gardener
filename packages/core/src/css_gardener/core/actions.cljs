(ns css-gardener.core.actions
  (:refer-clojure :exclude [apply])
  (:require [clojure.core.async :refer [go]]
            [css-gardener.core.caching :as caching]
            [css-gardener.core.utils.async :as a]))

(defmulti apply
  "Performs side-effects in response to an action."
  (fn [_ action] (:type action)))

(defn update-dependency-graph
  "Action indicating that the dependency graph should be updated starting from
   the passed in node."
  [absolute-path]
  {:type ::update-dependency-graph :absolute-path absolute-path})

(defn remove-from-cache
  "Action indicating that the given file should be removed from the compilation
   cache."
  [absolute-path]
  {:type ::remove-from-cache :absolute-path absolute-path})

(defmethod apply ::remove-from-cache
  [{:keys [compilation-cache]} {:keys [absolute-path]}]
  (->> (caching/remove compilation-cache absolute-path)
       (a/map (constantly ::done))))

(defn recompile
  "Action indicating that the outputs should be recompiled."
  []
  {:type ::recompile})

(defmethod apply :default
  [_ {:keys [type]}]
  (go (js/Error. (str "Unknown action type " type))))
