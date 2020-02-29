(ns xyz.dking.css-gardener.utils.async
  (:refer-clojure :exclude [map])
  (:require [clojure.core.async :refer [go go-loop >! chan put! close! pipe]]
            [xyz.dking.css-gardener.utils.errors :as errors]))

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
  (transform (cljs.core/map f) ch))