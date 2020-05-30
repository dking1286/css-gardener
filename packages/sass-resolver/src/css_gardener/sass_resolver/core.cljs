(ns css-gardener.sass-resolver.core
  (:require [goog.array :as array]
            [goog.object :as gobj]
            [sass]))

(defn main
  "Dependency resolver for scss and sass stylesheets."
  [file options ^js/Function callback]
  (cond
    (not (gobj/containsKey file "absolutePath"))
    (callback (js/Error. "'absolutePath' key is missing on the input file") nil)

    (not (gobj/containsKey file "content"))
    (callback (js/Error. "'content' key is missing on the input file") nil)

    :else
    (let [absolute-path (gobj/get file "absolutePath")
          content (gobj/get file "content")]
      (sass/render
       (js/Object.assign #js {:file absolute-path
                              :data content}
                         options)
       (fn [err result]
         (if err
           (callback err nil)
           ;; TODO: This will cause some files to be
           ;; duplicated in the dependency graph, because
           ;; dependencies of the file.
           ;; includedFiles includes all *transitive*
           ;; Not a big deal, because sass files usually
           ;; don't have a deep dependency graph.
           ;; 
           ;; A better long-term solution: Contribute to
           ;; sass-graph to make it support the new
           ;; @use syntax.
           ;; https://www.npmjs.com/package/sass-graph
           (callback nil (-> result
                             (gobj/get "stats")
                             (gobj/get "includedFiles")
                             (array/filter #(not= % absolute-path))))))))))
