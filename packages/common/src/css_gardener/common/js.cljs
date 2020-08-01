(ns css-gardener.common.js
  (:require [goog.string]))

(defn from-js
  "Converts a js data structure to a cljs data structure, changing string
   keys to keywords.
   
   Converts camelCase string keys to selector-case keywords."
  [data]
  (->> (js->clj data)
       (map (fn [[k v]] [(keyword (goog.string/toSelectorCase k)) v]))
       (into {})))

(defn to-js
  "Converts a cljs data structure to a js data structure, changing keyword
   keys to strings.
   
   Converts selector-case keyword keys to camelCase."
  [data]
  (clj->js data :keyword-fn (comp goog.string/toCamelCase name)))