(ns xyz.dking.css-gardener.utils.errors
  (:require [clojure.string :as string]))

(defn not-found
  "Creates an error indicating that a requested resource was not found."
  [cause]
  (ex-info "The requested resource was not found"
           {:type :not-found}
           cause))

(defn not-found?
  "Determines if an error is of type :not-found"
  [err]
  (= :not-found (-> err ex-data :type)))

(defn unexpected-error
  [cause]
  (ex-info "An unexpected error occurred"
           {:type :unexpected}
           cause))

(defn- error-message-includes?
  [err substring]
  (string/includes? (.-message err) substring))

(defn wrap-node-error
  "Wraps a native nodejs error into one of the ex-info types defined in this
  file."
  [err]
  (cond
    (nil? err) nil
    (error-message-includes? err "no such file or directory") (not-found err)
    :else (unexpected-error err)))