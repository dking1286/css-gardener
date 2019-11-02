(ns xyz.dking.css-gardener.core
  (:require [clojure.java.io :as io]
            [cljs.env :as env]
            [cljs.compiler.api :as compiler]
            [cljs.build.api :as build]
            [cljs.analyzer.api :as analyzer]
            [me.raynes.fs :as fs]))

(def core-ns-names #{'cljs.core 'cljs.user})

(defn core-ns?
  [name]
  (core-ns-names name))

(defn analyze-file-ns
  [file]
  (analyzer/with-state (analyzer/empty-state)
    (do (analyzer/analyze-file file)
        (->> @env/*compiler*
             :cljs.analyzer/namespaces
             (filter (fn [[name ns-map]] (not (core-ns? name))))
             vals
             first))))

(defn style-vars
  [ns-map]
  (->> ns-map
       :defs
       vals
       (filter :css-gardener/style)
       (map :name)))

;; (style-vars (analyze-file-ns (io/file "example/example.cljs")))
