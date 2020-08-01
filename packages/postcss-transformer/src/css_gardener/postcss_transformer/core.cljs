(ns css-gardener.postcss-transformer.core
  (:require [css-gardener.common.node-modules :refer [root-module-path]]
            [goog.object :as gobj]
            ["postcss" :as postcss]))

(defn- load-plugin
  [plugin-spec]
  (let [module (-> (gobj/get plugin-spec "nodeModule")
                   root-module-path
                   js/require)]
    (if (gobj/containsKey plugin-spec "options")
      (module (gobj/get plugin-spec "options"))
      module)))

(defn- load-plugins
  [plugin-specs]
  (->> plugin-specs
       (map load-plugin)
       js/Array.from))

(defn enter
  "Entry point for the postcss transformer."
  [file options callback]
  (cond
    (not (gobj/containsKey file "absolutePath"))
    (callback (js/Error. "'absolutePath' key is missing on the input file") nil)

    (not (gobj/containsKey file "content"))
    (callback (js/Error. "'content' key is missing on the input file") nil)

    :else
    (let [plugins (load-plugins (gobj/get options "plugins"))]
      (-> (postcss plugins)
          (.process (gobj/get file "content")
                    ;; TODO: Make these not undefined for source mapping
                    ;; purposes
                    #js {:from js/undefined :to js/undefined})
          (.then (fn [result]
                   (let [outfile (gobj/clone file)]
                     (gobj/set outfile "content" (gobj/get result "css"))
                     (callback nil outfile))))))))

(defn exit
  "Exit point for the postcss transformer. Returns the file unmodified."
  [file _ callback]
  (callback nil file))
