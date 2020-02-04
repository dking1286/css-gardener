(ns xyz.dking.css-gardener.builder.sass
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [xyz.dking.css-gardener.builder :refer [Builder]]))

(defn compile-sass
  "Compiles a single scss file. Returns a map of the form
  {:file input file name
   :result css string, or nil if compilation was unsuccessful
   :error error that occurred while compiling, or nil if successful"
  [{:keys [file text] :as file-info}]
  (let [in-uri (.toURI (io/file file))
        out-uri (.toURI (io/file (str ".css-gardener/scss-out/"
                                      "output-from-" file ".css")))
        compiler (io.bit3.jsass.Compiler.)
        options (io.bit3.jsass.Options.)]
    (try
      (assoc file-info :result (-> compiler
                                   (.compileString text in-uri out-uri options)
                                   (.getCss)))
      (catch io.bit3.jsass.CompilationException e
        (assoc file-info :error e)))))

(defrecord ScssBuilder []
  Builder
  (build-file [this file-details]
    (compile-sass file-details)))

(defn new-builder
  [_]
  (->ScssBuilder))

