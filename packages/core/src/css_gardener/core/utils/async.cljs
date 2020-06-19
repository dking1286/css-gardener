(ns css-gardener.core.utils.async
  (:refer-clojure :exclude [constantly map merge])
  (:require [clojure.core.async
             :refer [go go-loop chan put! close! pipe alts! timeout merge
                     <! >!]]
            [css-gardener.core.logging :as logging]
            [css-gardener.core.utils.errors :as errors]))

(defn callback->channel
  "Takes a function that takes a callback, and returns a channel that yields
   the value returned by the callback. Used to convert callback-based async
   functions into channel-based."
  [f & args]
  (let [out-chan (chan)]
    (go
      (let [f-args (vec (butlast args))
            transformer (last args)
            callback (fn [& cb-args]
                       (let [out-val (try
                                       (apply transformer cb-args)
                                       (catch js/Error err
                                         err))]
                         (put! out-chan out-val)
                         (close! out-chan)))]
        (try
          (apply f (conj f-args callback))
          (catch js/Error err
            (put! out-chan err)
            (close! out-chan)))))
    out-chan))

(defn- wrap-error-first-fn
  [f]
  (fn [err & args]
    (apply f (errors/wrap-node-error err) args)))

(defn node-callback->channel
  "Takes a function that takes a node-style error-first callback, and returns
   a channel that yields the value returned by the callback. Used to convert
   a node-style callback-based async function into channel-based."
  [f & args]
  (let [f-args (vec (butlast args))
        transformer (wrap-error-first-fn (last args))]
    (apply callback->channel f (conj f-args transformer))))

(defn- transform
  [xform ch]
  (let [out (chan 1 xform)]
    (pipe ch out)
    out))

(defn map
  "Takes a function and a channel, and returns a channel that yields all values
   from the input channel, transformed with the function.
   
   If the input channel yields an error, it is propagated to the returned
   channel without being transformed."
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
  "Calls f with each value from ch, and returns a channel that yields all values
   yielded by the channels returned by f.
   
   (a -> Channel b) -> Channel a -> Channel b"
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

(defn then
  "Calls f with each value from ch, returns a channel that yields nothing
   unless an error occurs, in which case the error is yielded.
   
   f should be a function with side effects, otherwise there is not much point
   to using this function."
  [f ch]
  (->> ch
       (flat-map f)
       (transform (filter errors/error?))))

(defn trace
  "Returns a channel that prints and then yields each value from the input
   channel.
   
   Useful for debugging chains of async operations."
  ([ch] (trace nil ch))
  ([prefix ch]
   (transform (cljs.core/map #(logging/trace prefix %)) ch)))

(defn take-all
  "Repeatedly takes from the input channel until it closes, then yields a
   single seq of all values received from the input channel."
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
  "Waits for all of the input channels to close, then yields a seq of all
   values received from them. If any of the input channels yields an error,
   immediately yield the error."
  [timeout-millis chs]
  (take-all timeout-millis (merge chs) :bailout-on-error? true))