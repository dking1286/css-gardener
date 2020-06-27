(ns css-gardener.core.caching)

(defmacro with-cache
  "Convenience macro for executing expensive computations with a cache."
  [cache key & body]
  `(compute-with-cache ~cache ~key (fn [] ~@body)))