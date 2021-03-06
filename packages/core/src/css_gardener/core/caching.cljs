(ns css-gardener.core.caching
  (:refer-clojure :exclude [get remove set])
  (:require-macros [css-gardener.core.caching])
  (:require [clojure.core.async :refer [go]]
            [clojure.tools.namespace.dependency :as ctnd]
            [css-gardener.core.utils.async :as a]
            [integrant.core :as ig]))

(defprotocol ICache
  "Protocol representing a key-value cache."
  (get [this key]
    "Gets the value associated with `key`.")
  (set [this key value]
    "Sets the value of `key` to `value`.")
  (remove [this key]
    "Removes `key` from the cache."))

(defrecord ^{:doc "In memory implementation of a key-value cache."}
  InMemoryCache
  [cache-atom]
  ICache
  (get [_ key]
    (go (or (clojure.core/get @cache-atom key)
            ::not-found)))
  (set [_ key value]
    (swap! cache-atom assoc key value)
    (go [key value]))
  (remove [_ key]
    (swap! cache-atom dissoc key)
    (go ::done)))

(defn new-in-memory-cache
  "Creates a new InMemoryCache instance."
  [initial-state]
  (->InMemoryCache (atom initial-state)))

(defn found?
  "Determines if a value was found in the cache."
  [value]
  (not= value ::not-found))

(defn compute-with-cache
  "Returns a channel that yields the cached value associated with a key.
   If the key does not exist in the cache, evaluate the zero-argument function
   f, reads from the channel returned by f, sets that value in the cache, and
   returns the value."
  [cache key f]
  (->> (get cache key)
       (a/flat-map
        (fn [cached]
          (if (found? cached)
            (go cached)
            (->> (f)
                 (a/flat-map
                  (fn [result]
                    (->> (set cache key result)
                         (a/map (constantly result)))))))))))

(defmethod ig/init-key ::compilation-cache
  [_ {initial-state :initial-state
      :or
      {initial-state {}}}]
  (new-in-memory-cache initial-state))

(defmethod ig/init-key ::dependency-graph-cache
  [_ _]
  (atom (ctnd/graph)))