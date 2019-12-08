(ns xyz.dking.css-gardener.utils
  (:require [me.raynes.fs :as fs])
  (:import [java.util UUID]))

(defn map-vals
  "Maps over a seq of key-value pairs, applying the mapping function to
  each value."
  [f & args]
  (apply map (fn [[key val]] [key (f val)]) args))

(defn unique-by
  "Gets a sequence of unique values in a seq, with uniqueness
  determined by a passed-in function."
  ([f seq] (unique-by f seq #{}))
  ([f seq seen]
   (cond
     (nil? seq) nil
     (empty? seq) '()
     :else
     (let [next-item (first seq)
           next-key (f next-item)]
       (if (seen next-key)
         (recur f (rest seq) seen)
         (lazy-seq (cons next-item
                         (unique-by f (rest seq) (conj seen next-key)))))))))

(defn unique-files
  "Gets a seq of all the unique files matching a seq of globs."
  [globs]
  (->> globs
       (mapcat fs/glob)
       (sort-by #(.getName %))
       (unique-by #(.getName %))))

(defn uuid
  "Creates a uuid string."
  []
  (.toString (UUID/randomUUID)))
