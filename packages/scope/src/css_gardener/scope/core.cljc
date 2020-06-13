(ns css-gardener.scope.core
  #?(:clj (:require [cljs.analyzer :as ana]
                    [clojure.java.io :as io]
                    [css-gardener.common.constants :as constants]))
  #?(:cljs (:require-macros [css-gardener.scope.core])))

(defn scope-from-stylesheet
  "Gets the scope from a style file's contents."
  [file-content]
  (second (re-find #"\:css-gardener/scope\s+\"(\S+)\"" file-content)))

(comment
  (scope-from-stylesheet "{:css-gardener/scope \"hello\"}"))

#?(:clj
   (defn- scope-from-style-dep
     [current-file dep]
     (-> (io/file current-file ".." dep)
         (.getCanonicalPath)
         slurp
         scope-from-stylesheet)))

#?(:clj
   (defn scope-from-style-deps
     "Reads the scope from a list of style dependencies."
     [current-file deps]
     (let [scopes (into #{} (map #(scope-from-style-dep current-file %)) deps)]
       (if (= 1 (count scopes))
         (first scopes)
         (throw (ex-info (str "Multiple scopes found in style dependencies of "
                              current-file
                              ": "
                              scopes)
                         {:type :multiple-scopes
                          :file current-file
                          :scopes scopes}))))))

#?(:clj
   (defmacro infer-scope
     "Infer the scope for the current cljs file based on the scopes declared
      in the style dependencies."
     []
     (scope-from-style-deps
      ana/*cljs-file*
      (constants/require-metadata-key (meta ana/*cljs-ns*)))))