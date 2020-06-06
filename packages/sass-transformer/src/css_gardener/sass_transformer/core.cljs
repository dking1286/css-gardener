(ns css-gardener.sass-transformer.core
  (:require [goog.object :as object]
            [sass]))

(defn- object-merge
  "Merges multiple js objects into one."
  [& objs]
  (apply js/Object.assign #js {} objs))

(defn enter
  "Entry point for the sass transformer."
  [file options callback]
  (cond
    (not (object/containsKey file "absolutePath"))
    (callback (js/Error. "'absolutePath' key is missing on the input file") nil)

    (not (object/containsKey file "content"))
    (callback (js/Error. "'content' key is missing on the input file") nil)

    :else
    (let [absolute-path (object/get file "absolutePath")
          content (object/get file "content")]
      (sass/render
       (object-merge #js {:file absolute-path
                          :data content
                          :outputStyle "expanded"}
                     options)
       (fn [err result]
         (if err
           (callback err nil)
           (let [out-content (.toString (object/get result "css"))
                 out-file
                 (object-merge file
                               #js {:content out-content
                                    :sassTransformerOriginalContent content})]
             (callback nil out-file))))))))

(defn exit
  "Exit point for the sass transformer. Returns the file unmodified."
  [file _ callback]
  (callback nil file))