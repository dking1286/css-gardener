(ns css-gardener.scope.core)

(defn scope-from-stylesheet
  "Gets the scope from a style file's contents."
  [file-content]
  (second (re-find #"\:css-gardener/scope\s+\"(\S+)\"" file-content)))

(comment
  (scope-from-stylesheet "{:css-gardener/scope \"hello\"}"))
