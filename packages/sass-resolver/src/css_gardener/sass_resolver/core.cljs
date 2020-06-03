(ns css-gardener.sass-resolver.core
  (:require [fs]
            [goog.object :as gobj]
            [path]))

(def ^:private import-regexp #"@import ('|\")(\S+)('|\")")
(def ^:private use-regexp #"@use ('|\")(\S+)('|\")")

(defn- raw-dependency-paths
  [file]
  (let [content (gobj/get file "content")]
    (->> (re-seq import-regexp content)
         (map #(nth % 2))
         (concat (->> (re-seq use-regexp content)
                      (map #(nth % 2))))
         set)))

(defn- add-extension
  [extension dependency-path]
  (if (re-find #"\.\w+$" dependency-path)
    dependency-path
    (str dependency-path extension)))

(defn- relative->absolute
  [file dependency-path]
  (if (path/isAbsolute dependency-path)
    dependency-path
    (path/resolve (path/dirname (gobj/get file "absolutePath"))
                  dependency-path)))

(comment
  (re-seq import-regexp
          "@import 'hello/world';
           @import 'some/other/path';")
  (raw-dependency-paths #js {:absolutePath ""
                             :content "@import 'some/path';
                                     @use 'some/other/path';"})
  (relative->absolute #js {:absolutePath "/some/path"
                           :content ""}
                      "../some/other/path"))

(defn main
  "Dependency resolver for scss and sass stylesheets."
  [file options ^js/Function callback]
  (cond
    (not (gobj/containsKey file "absolutePath"))
    (callback (js/Error. "'absolutePath' key is missing on the input file") nil)

    (not (gobj/containsKey file "content"))
    (callback (js/Error. "'content' key is missing on the input file") nil)

    :else
    (try
      (let [indented-syntax? (or (gobj/get options "indentedSyntax")
                                 false)
            extension (if indented-syntax? ".sass" ".scss")]
        (callback nil (->> (raw-dependency-paths file)
                           (map #(add-extension extension %))
                           (map #(relative->absolute file %))
                           set
                           js/Array.from)))
      (catch js/Error err
        (callback err nil)))))
