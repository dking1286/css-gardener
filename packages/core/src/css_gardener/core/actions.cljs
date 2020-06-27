(ns css-gardener.core.actions)

(defn update-dependency-graph
  "Action indicating that the dependency graph should be updated starting from
   the passed in node."
  [absolute-path]
  {:type ::update-dependency-graph :absolute-path absolute-path})

(defn remove-from-cache
  "Action indicating that the given file should be removed from the compilation
   cache."
  [absolute-path]
  {:type ::remove-from-cache :absolute-path absolute-path})

(defn recompile
  "Action indicating that the outputs should be recompiled."
  []
  {:type ::recompile})