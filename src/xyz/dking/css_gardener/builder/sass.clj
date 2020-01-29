(ns xyz.dking.css-gardener.builder.sass
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [hawk.core :as hawk]
            [xyz.dking.css-gardener.builder :refer [Builder]]
            [xyz.dking.css-gardener.logging :as logging]
            [xyz.dking.css-gardener.utils :as utils]
            [clojure.spec.alpha :as s]))

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

(defn sass-file?
  [file]
  (let [filename (.getName file)]
    (str/ends-with? filename ".scss")))

(defrecord ScssBuilder [watcher cached-files]
  Builder
  (start [this]) ;; Do nothing                                       
  
  (stop [this]
    (when-let [w @watcher]
      (hawk/stop! w)
      (reset! watcher nil)))
  
  (build [this config]
    (pmap compile-sass (:unique-input-files config)))
  
  (watch [this config on-change])) ;; Implement me

(defn new-builder
  [_]
  (->ScssBuilder (atom nil) (atom {})))

