(ns xyz.dking.css-gardener.v1.utils.seq)

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

(def unique (partial unique-by identity))