(ns css-gardener.scripts.remove-build-caches
  (:refer-clojure :exclude [exists?])
  (:require [css-gardener.scripts.utils :refer [exists?
                                                get-packages
                                                remove-dir]]))

(def ^:private build-caches #{".cpcache"
                              ".shadow-cljs"
                              "dist"
                              "target"})

(defn -main
  [& _]
  (try
    (doseq [package (get-packages)
            build-cache build-caches]
      (let [cache-path (str package "/" build-cache)]
        (if (exists? cache-path)
          (do
            (println (str "Removing build cache " cache-path))
            (remove-dir cache-path))
          (println (str "No build cache found at " cache-path ", skipping.")))))
    (catch Exception err
      (println (str "Error: " (.-message err)))
      (System/exit 1))))