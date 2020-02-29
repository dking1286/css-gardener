(ns xyz.dking.css-gardener.utils.async
  (:require [clojure.core.async :refer [go >! chan put!]]
            [xyz.dking.css-gardener.utils.errors :as errors]))

(defn callback->channel
  [f & args]
  (let [out-chan (chan)]
    (go
      (let [f-args (vec (butlast args))
            transformer (last args)
            callback (fn [& cb-args]
                       (put! out-chan (apply transformer cb-args)))]
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