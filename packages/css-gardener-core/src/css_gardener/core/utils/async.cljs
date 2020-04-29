(ns css-gardener.core.utils.async
  (:refer-clojure :exclude [constantly map])
  (:require [clojure.core.async :refer [go go-loop chan put! close! pipe alts! timeout]]
            [css-gardener.core.utils.errors :as errors]))

(defn constantly
  [val]
  (fn [& _] (go val)))

(defn callback->channel
  [f & args]
  (let [out-chan (chan)]
    (go
      (let [f-args (vec (butlast args))
            transformer (last args)
            callback (fn [& cb-args]
                       (put! out-chan (apply transformer cb-args))
                       (close! out-chan))]
        (apply f (conj f-args callback))))
    out-chan))

(defn- wrap-error-first-fn
  [f]
  (fn [err & args]
    (apply f (errors/wrap-node-error err) args)))

(defn node-callback->channel
  [f & args]
  (let [f-args (vec (butlast args))
        transformer (wrap-error-first-fn (last args))]
    (apply callback->channel f (conj f-args transformer))))

(defn transform
  [xform ch]
  (let [out (chan 1 xform)]
    (pipe ch out)
    out))

(defn map
  [f ch]
  (transform (cljs.core/map (fn [x]
                              (if (ex-data x)
                                x
                                (f x))))
             ch))

(defn trace
  ([ch] (trace nil ch))
  ([prefix ch]
   (transform (cljs.core/map (fn [x]
                               (if prefix
                                 (println (str prefix x))
                                 (println x))
                               x))
              ch)))

(defn take-all
  [timeout-millis ch]
  (let [timeout-ch (timeout timeout-millis)]
    (go-loop [results []]
      (let [[result port] (alts! [ch timeout-ch])]
        (if (identical? port timeout-ch)
          (errors/deadline-exceeded)
          (if (nil? result)
            results
            (recur (conj results result))))))))