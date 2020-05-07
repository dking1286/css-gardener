(ns css-gardener.core.utils.async
  (:refer-clojure :exclude [constantly map merge])
  (:require [clojure.core.async :refer [go go-loop chan put! close! pipe alts! timeout merge <! >!]]
            [css-gardener.core.utils.errors :as errors]))

(defn constantly
  "Returns a function that always returns a channel containing the specified 
  value."
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
                              (if (errors/error? x)
                                x
                                (try
                                  (f x)
                                  (catch js/Error err
                                    err)))))
             ch))

(defn flat-map
  [f ch]
  (let [out-chan (chan)]
    (go-loop []
      (let [in-value (<! ch)]
        (cond
          (nil? in-value) (close! out-chan)
          (errors/error? in-value) (>! out-chan in-value)
          :else
          (let [out-value (try
                            (<! (f in-value))
                            (catch js/Error err
                              err))]
            (>! out-chan out-value)
            (recur)))))
    out-chan))

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
  [timeout-millis ch & {:keys [bailout-on-error?]}]
  (let [timeout-ch (timeout timeout-millis)]
    (go-loop [results []]
      (let [[result port] (alts! [ch timeout-ch])]
        (cond
          (identical? port timeout-ch) (errors/deadline-exceeded)
          (nil? result) results
          (and bailout-on-error? (instance? js/Error result)) result
          :else (recur (conj results result)))))))

(defn await-all
  [timeout-millis chs]
  (take-all timeout-millis (merge chs) :bailout-on-error? true))