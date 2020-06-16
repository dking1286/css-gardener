(ns css-gardener.scope-transformer.core
  (:require [clojure.string :as string]
            [css-gardener.common.object :refer [object-merge]]
            [css-gardener.scope.core :refer [scope-from-stylesheet
                                             scoped-classname]]
            [goog.object :as object]))

(def ^{:private true
       :doc "Regular expression that matches css class selectors, capturing
             the name.
             
             CSS identifiers may contain any characters except the following:
             * May not contain ascii characters other than a-z A-Z 0-9 - _
             * May not start with a digit, two dashes, or a dash then a digit.
             
             This regexp excludes all of the disallowed possibilities.
             
             TODO: Support escape sequences in selectors.
             
             See https://www.w3.org/TR/css-syntax-3/#consume-name"}
  class-regexp
  #"\.(-?[^-\s!\"#\$%\&'\(\)\*\+,\./:;<=>\?@\[\\\]\^`{\|}~0-9]+[^\s!\"#\$%\&'\(\)\*\+,\./:;<=>\?@\[\\\]\^`{\|}~]*)")

(defn- scoped-content
  [scope content]
  (string/replace content
                  class-regexp
                  #(str "." (scoped-classname scope (second %)))))

(defn enter
  "Entry function for the scope transformer."
  [file _ callback]
  (let [scope (scope-from-stylesheet (object/get file "content"))
        outfile (if scope
                  (object-merge file #js {:scopeTransformerScope scope})
                  file)]
    (callback nil outfile)))

(defn exit
  "Exit function for the scope transformer."
  [file _ callback]
  (if (object/containsKey file "scopeTransformerScope")
    (let [scope (object/get file "scopeTransformerScope")
          content (object/get file "content")
          transformed (object-merge
                       file #js {:content (scoped-content scope content)})]
      (callback nil transformed))
    (callback nil file)))
