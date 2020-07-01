(ns css-gardener.core.change-detection
  (:require [chokidar]
            [clojure.core.async :refer [chan close! put!]]
            [clojure.spec.alpha :as s]
            [clojure.tools.namespace.dependency :as ctnd]
            [css-gardener.core.actions :as actions]
            [css-gardener.core.config :as config]
            [css-gardener.core.dependency :as dependency]
            [css-gardener.core.file :as file]
            [css-gardener.core.logging :as logging]
            [css-gardener.core.transformation :as transformation]
            [integrant.core :as ig]))

(defmethod ig/init-key ::input-channel
  [_ {:keys [buffer] :or {buffer 100}}]
  (chan buffer))

(defmethod ig/halt-key! ::input-channel
  [_ input-channel]
  (close! input-channel))

(def ^:private dotfiles-regexp #"(^|[\/\\])\..")

(defmethod ig/init-key ::watcher
  [_ {:keys [watch? logger config input-channel]}]
  (when (and watch? (seq (:source-paths config)))
    (logging/debug logger "Starting file watcher")
    (let [watcher (-> (chokidar/watch (clj->js (:source-paths config))
                                      #js {:ignored dotfiles-regexp
                                           :ignoreInitial true})
                      (.on "add"
                           #(put! input-channel {:type :add :path %}))
                      (.on "change"
                           #(put! input-channel {:type :change :path %}))
                      (.on "unlink"
                           #(put! input-channel {:type :unlink :path %})))]
      {:watcher watcher :logger logger})))

(defmethod ig/halt-key! ::watcher
  [_ {:keys [watcher logger]}]
  (when watcher
    (logging/debug logger "Stopping file watcher")
    (.close watcher)))

(defn- in-dependency-graph?
  [dependency-graph absolute-path]
  (contains? (ctnd/nodes dependency-graph) absolute-path))

(defn- deps-have-changed?
  [dependency-graph absolute-path new-deps]
  (not= (ctnd/immediate-dependencies dependency-graph absolute-path)
        new-deps))

(s/fdef get-actions
  :args (s/cat :config ::config/config
               :dependency-graph ::dependency/dependency-graph
               :absolute-path ::file/absolute-path
               :new-deps (s/coll-of ::file/absolute-path :kind set?)))

(defn get-actions
  "Gets the sequence of actions that should be applied as a result of a change
   to one of the files in the project."
  [config dependency-graph absolute-path new-deps]
  (if (in-dependency-graph? dependency-graph absolute-path)
    (if (transformation/style-file? config absolute-path)
      (if (transformation/root-style-file? config
                                           dependency-graph
                                           absolute-path)
        (if (deps-have-changed? dependency-graph absolute-path new-deps)
          [(actions/->UpdateDependencyGraph absolute-path)
           (actions/->RemoveFromCache absolute-path)
           (actions/->Recompile)]
          [(actions/->RemoveFromCache absolute-path)
           (actions/->Recompile)])
        (let [root-styles (transformation/get-root-style config
                                                         dependency-graph
                                                         absolute-path)]
          (if (deps-have-changed? dependency-graph absolute-path new-deps)
            (vec (concat [(actions/->UpdateDependencyGraph absolute-path)]
                         (map actions/->RemoveFromCache root-styles)
                         [(actions/->Recompile)]))
            (vec (concat (map actions/->RemoveFromCache root-styles)
                         [(actions/->Recompile)])))))
      (if (deps-have-changed? dependency-graph absolute-path new-deps)
        [(actions/->UpdateDependencyGraph absolute-path)
         (actions/->Recompile)]
        []))
    []))
