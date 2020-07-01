(ns css-gardener.core.actions
  (:refer-clojure :exclude [apply])
  (:require [clojure.core.async :refer [go-loop <! put!]]
            [clojure.tools.namespace.dependency :as ctnd]
            [css-gardener.core.arguments :as arguments]
            [css-gardener.core.caching :as caching]
            [css-gardener.core.dependency :as dependency]
            [css-gardener.core.logging :as logging]
            [css-gardener.core.output :as output]
            [css-gardener.core.transformation :as transformation]
            [css-gardener.core.utils.async :as a]))

(defprotocol IAction
  (apply [this system]))

(defrecord CreateDependencyGraph []
  IAction
  (apply [_ {build-id ::arguments/build-id
             deps-graph ::dependency/deps-graph
             dependency-graph-cache ::caching/dependency-graph-cache}]
    (->> (deps-graph build-id)
         (a/map (fn [new-deps-graph]
                  (reset! dependency-graph-cache new-deps-graph)
                  ::done)))))

(defrecord UpdateDependencyGraph [absolute-path]
  IAction
  (apply [_ {build-id ::arguments/build-id
             deps-graph ::dependency/deps-graph
             dependency-graph-cache ::caching/dependency-graph-cache}]
    (let [initial-graph (ctnd/remove-node @dependency-graph-cache absolute-path)]
      (->> (deps-graph build-id
                       :initial-graph initial-graph
                       :entry-files [absolute-path])
           (a/map (fn [new-deps-graph]
                    (reset! dependency-graph-cache new-deps-graph)
                    ::done))))))

(defrecord RemoveFromCache [absolute-path]
  IAction
  (apply [_ {compilation-cache ::caching/compilation-cache}]
    (->> (caching/remove compilation-cache absolute-path)
         (a/map (constantly ::done)))))

(defrecord CompileOnce []
  IAction
  (apply [_ {build-id ::arguments/build-id
             dependency-graph-cache ::caching/dependency-graph-cache
             compile-all ::transformation/compile-all
             logger ::logging/logger}]
    (->> (compile-all build-id @dependency-graph-cache)
         (a/flat-map #(go-loop [[outfile outfiles] %]
                        (when outfile
                          (<! (output/write-output logger outfile))
                          (recur outfiles)))))))

(defrecord Recompile []
  IAction
  (apply [_ {build-id ::arguments/build-id
             dependency-graph-cache ::caching/dependency-graph-cache
             compile-all ::transformation/compile-all
             output-channel ::output/output-channel}]
    (->> (compile-all build-id @dependency-graph-cache)
         (a/then #(doseq [outfile %]
                    (put! output-channel outfile))))))

(defn apply-all
  [{logger ::logging/logger :as sys} actions]
  (go-loop [[action & remaining] actions]
    (when action
      (logging/debug logger (str "Applying action " (pr-str action)))
      (<! (apply action sys))
      (recur remaining))))