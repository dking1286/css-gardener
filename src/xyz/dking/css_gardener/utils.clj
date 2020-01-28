(ns xyz.dking.css-gardener.utils
  (:require [clojure.spec.alpha :as s]
            [clojure.string :as str]
            [me.raynes.fs :as fs])
  (:import java.util.UUID))

(s/fdef trace
  :args (s/cat :message (s/? (s/nilable string?)) :val any?)
  :fn #(= (:ret %) (-> % :args :val)))

(defn trace
  "Logs a value and returns it unchanged."
  ([val] (trace nil val))
  ([message val]
   (when message (print message))
   (println val)
   val))

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

(defn globstar
  "Returns a seq of files matching a recursive glob containing **."
  [glob]
  (let [star-stars (re-seq #"\/\*\*\/" glob)]
    (cond
      (= (count star-stars) 0) (fs/glob glob)

      (>= (count star-stars) 2)
      (throw (ex-info "Multiple \"**\" expansions not supported."
                      {:type :multiple-star-star-not-supported}))
                                        
      :else
      (let [[beginning end] (str/split glob #"\/\*\*\/")]
        (->> (fs/walk (fn [root dirs files] root) beginning)
             (mapcat #(fs/glob % end)))))))

(defn unique-files
  "Gets a seq of all the unique files matching a seq of globs."
  [globs]
  (->> globs
       (mapcat globstar)
       (sort-by #(.getName %))
       (unique-by #(fs/absolute %))))

(defn uuid
  "Creates a uuid string."
  []
  (.toString (UUID/randomUUID)))
