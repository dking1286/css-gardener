(ns css-gardener.core.utils.errors
  (:require [clojure.string :as string]))

(defn error?
  "Determines if a value is an error."
  [val]
  (instance? js/Error val))

(defn message
  "Gets the message from an error as a string."
  [err]
  (.-message err))

(defn stack
  "Gets the stack trace from an error as a string."
  [err]
  (.-stack err))

(defn invalid-config
  "Creates an error indicating that the configuration was invalid."
  ([message] (ex-info message {:type :invalid-config}))
  ([message cause] (ex-info message {:type :invalid-config} cause)))

(defn invalid-config?
  "Determines if an error is of type :invalid-config."
  [err]
  (= :invalid-config (-> err ex-data :type)))

(defn invalid-dependency-resolver
  "Creates an error indicating that a dependency resolver was invalid"
  ([message] (ex-info message {:type :invalid-dependency-resolver}))
  ([message cause] (ex-info message {:type :invalid-dependency-resolver} cause)))

(defn invalid-dependency-resolver?
  "Determines if an error is of type :invalid-dependency-resolver"
  [err]
  (= :invalid-dependency-resolver (-> err ex-data :type)))

(defn not-found
  "Creates an error indicating that a requested resource was not found."
  ([message] (ex-info message {:type :not-found}))
  ([message cause] (ex-info message {:type :not-found} cause)))

(defn not-found?
  "Determines if an error is of type :not-found"
  [err]
  (= :not-found (-> err ex-data :type)))

(defn conflict
  "Creates an error indicating that a uniqueness constraint was violated"
  [message]
  (ex-info (str "Conflict: " message)
           {:type :conflict}))

(defn conflict?
  "Determines if an error is of type :conflict."
  [err]
  (= :conflict (-> err ex-data :type)))

(defn deadline-exceeded
  "Creates an error indicating that an async operation timed out."
  ([] (deadline-exceeded "Deadline exceeded"))
  ([message]
   (ex-info (str message)
            {:type :deadline-exceeded})))

(defn deadline-exceeded?
  "Determines if an error is a deadline exceeded error."
  [err]
  (= :deadline-exceeded (-> err ex-data :type)))

(defn unexpected-error
  "Creates an error indicating that something unknown went wrong."
  ([cause] (unexpected-error "An unexpected error occurred" cause))
  ([message cause] (ex-info message {:type :unexpected} cause)))

(defn unexpected-error?
  "Determines if an error is an unexpected-error."
  [err]
  (= :unexpected (-> err ex-data :type)))

(defn- error-message-includes?
  [err substring]
  (string/includes? (.-message err) substring))

(defn wrap-node-error
  "Wraps a native nodejs error into one of the ex-info types defined in this
  file."
  [err]
  (cond
    (nil? err) nil

    (error-message-includes? err "no such file or directory")
    (not-found (message err) err)

    :else (unexpected-error err)))