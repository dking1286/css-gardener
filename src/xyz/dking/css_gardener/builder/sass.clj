(ns xyz.dking.css-gardener.builder.sass
  (:require [xyz.dking.css-gardener.builder :refer [Builder]]
            [xyz.dking.css-gardener.utils :as utils]
            [clojure.string :as str]
            [clojure.java.io :as io]
            [xyz.dking.css-gardener.logging :as logging]))


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
  (start [this]) ;; Do nothing                                       
  
  (stop [this]) ;; Do nothing
  
  (build [this config]
    (let [files (:unique-input-files config)
          compiled-files (pmap compile-sass files)]
      (if-some [error (first (filter :error compiled-files))]
        (logging/error (str "Error while compiling scss file " (:file error)
                            ": " (:error error)))
        (let [styles (map :result compiled-files)
              style-string (str/join "\n\n" styles)
              output-file (io/file (:output-file config))]
          (io/make-parents output-file)
          (spit output-file style-string)))))
  
  (watch [this config])) ;; To be implemented

(defn new-builder
  [_]
  (->ScssBuilder))

