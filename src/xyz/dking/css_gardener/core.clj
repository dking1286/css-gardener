(ns xyz.dking.css-gardener.core
  (:require [clojure.java.io :as io]
            [clojure.edn :as edn]
            [cljs.env :as env]
            [cljs.compiler.api :as compiler]
            [cljs.build.api :as build]
            [cljs.analyzer.api :as analyzer]
            [cemerick.pomegranate :as pomegranate]
            [me.raynes.fs :as fs]))

(def core-ns-names #{'cljs.core 'cljs.user})

(def cljs-index-filename
  ".css-gardener/xyz/dking/css_gardener/out/index.cljs")

(def cljs-index-ns
  'xyz.dking.css-gardener.out.index)

(def js-index-filename
  ".css-gardener/xyz/dking/css_gardener/out/index.js")

(def style-filename
  "resources/public/styles.css")

(defn core-ns?
  [name]
  (core-ns-names name))

(defn analyze-file-ns
  [file]
  (analyzer/with-state (analyzer/empty-state)
    (do
      (analyzer/analyze-file file)
      @env/*compiler*)))

(defn get-non-core-ns-map
  [compiler-state]
  (->> compiler-state
       :cljs.analyzer/namespaces
       (filter (fn [[name ns-map]] (not (core-ns? name))))
       vals
       first))

(defn style-vars
  [ns-map]
  (->> ns-map :defs vals (filter :css-gardener/style) (map :name)))

(defn all-style-vars
  [files]
  (->> files
       (map analyze-file-ns)
       (map get-non-core-ns-map)
       (mapcat style-vars)))

(def style-var->require-form
  (comp vector symbol namespace))

(defn style-index-content
  [index-ns style-out-filename style-vars]
  `((~'ns ~index-ns
     (:require [garden.core :as ~'garden]
                ~@(map style-var->require-form style-vars)))
 
   (def fs# (js/require "fs"))

    (let [style-str# (garden/css ~@style-vars)]
      (.writeFileSync fs# ~style-out-filename style-str#))))

(defn create-index-file
  [style-vars]
  (let [index-content (style-index-content cljs-index-ns
                                           style-filename
                                           style-vars)]
    (io/make-parents cljs-index-filename)
    ;; TODO: Move this somewhere more sensible
    (io/make-parents style-filename)
    (with-open [writer (io/writer cljs-index-filename)]
      (binding [*out* writer]
        (doseq [form index-content]
          (pr form))))))

(defn compile-index-file
  []
  (pomegranate/add-classpath ".css-gardener")

  (build/build {:main cljs-index-ns
                :output-to js-index-filename
                :output-dir ".css-gardener/xyz/dking/css_gardener/out/cljs_runtime"
                ;; TODO: This works, but it's slow. Make it faster somehow.
                ;; Either use optimizations :none, or maybe evaluate the code in
                ;; a node repl
                :optimizations :simple
                :target :nodejs}))

(defn main
  []
  (let [style-vars (all-style-vars (fs/glob "example/*.cljs"))]
    (create-index-file style-vars)
    (compile-index-file)))
