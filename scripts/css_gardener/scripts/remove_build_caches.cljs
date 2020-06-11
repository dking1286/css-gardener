(ns css-gardener.scripts.remove-build-caches
  (:refer-clojure :exclude [exists?])
  (:require [css-gardener.scripts.utils :refer [exists?
                                                json-parse
                                                remove-dir
                                                slurp]]))

(def ^:private build-caches #{".cpcache"
                              ".shadow-cljs"
                              "dist"
                              "target"})

(def ^:private lerna-json
  (json-parse (slurp "lerna.json")))

(try
  (doseq [package (get lerna-json "packages")
          build-cache build-caches]
    (let [cache-path (str package "/" build-cache)]
      (if (exists? cache-path)
        (do
          (println (str "Removing build cache " cache-path))
          (remove-dir cache-path))
        (println (str "No build cache found at " cache-path ", skipping.")))))
  (catch js/Error err
    (println (str "Error: " (.-message err)))
    (js/process.exit 1)))