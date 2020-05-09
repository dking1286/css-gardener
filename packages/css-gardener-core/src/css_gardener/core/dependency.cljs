(ns css-gardener.core.dependency
  (:require [clojure.core.async :refer [go]]
            [clojure.spec.alpha :as s]
            [clojure.tools.namespace.dependency :as dependency]
            [css-gardener.core.cljs-parsing :as cljs]
            [css-gardener.core.config :as config]
            [css-gardener.core.file :as file]
            [css-gardener.core.logging :as logging]
            [css-gardener.core.utils.async :as a]
            [css-gardener.core.utils.errors :as errors]
            [css-gardener.core.utils.spec :as su]
            [integrant.core :as ig]))

(s/def ::dependency-graph #(and (satisfies? dependency/DependencyGraph %)
                                (satisfies? dependency/DependencyGraphUpdate %)))

(s/fdef get-resolver
  :args (s/cat :load-module fn?
               :dependency-resolver ::config/dependency-resolver))

(defn- get-resolver
  [load-module dependency-resolver]
  (try
    (assoc dependency-resolver :resolver (load-module dependency-resolver))
    (catch js/Error err
      (errors/invalid-dependency-resolver "Failed to load module" err))))

(defn- resolve-deps
  [resolver resolver-name file]
  (a/node-callback->channel
   resolver file (fn [err deps]
                   (cond
                     err err

                     (js/Array.isArray deps) deps

                     :else
                     (errors/invalid-dependency-resolver
                      (str "Expected dependency resolver " resolver-name
                           " to yield an array of strings, got "
                           deps))))))

(s/fdef deps
  :args (s/cat :logger ::logging/logger
               :load-module fn?
               :cljs-deps fn?
               :file ::file/file
               :config ::config/config))

(defn- deps
  [;; Injected dependencies
   logger load-module cljs-deps
   ;; Arguments
   file config]
  (logging/debug logger (str "Getting dependencies of "
                             (:absolute-path file)))
  (let [rule-or-error (config/matching-rule config file)]
    (cond
      (cljs/cljs-file? file)
      (cljs-deps file (:source-paths config))

      (errors/not-found? rule-or-error)
      (go (errors/invalid-config (str "Problem finding rule for file "
                                      (:absolute-path file))
                                 rule-or-error))

      (errors/conflict? rule-or-error)
      (go (errors/invalid-config (str "Problem finding rule for file "
                                      (:absolute-path file))
                                 rule-or-error))

      (not (:dependency-resolver rule-or-error))
      (go #{})

      :else
      (->> (go (get-resolver load-module (:dependency-resolver rule-or-error)))
           (a/flat-map #(resolve-deps (:resolver %) (:node-module %) file))
           (a/map set)))))

(defmethod ig/init-key ::deps
  [_ {:keys [logger load-module cljs-deps fake-dependencies error]}]
  (cond
    ;; Mock deps with hard-coded map of dependencies
    fake-dependencies (fn [file _]
                        (go (or (get fake-dependencies (:absolute-path file))
                                #{})))
    ;; Mock deps that yields an error
    error (fn [_ _] (go error))
    ;; Real deps implementation
    :else (partial deps logger load-module cljs-deps)))

(defn get-entries
  "Gets all entry namespaces from a config map."
  [config build-id]
  (->> (get-in config [:builds build-id :modules])
       vals
       (mapcat :entries)
       set))

(s/fdef add-deps-for-path
  :args (s/cat :deps fn?
               :read-file fn?
               :graph (su/deref-of ::dependency-graph)
               :config ::config/config
               :path string?))

(defn- add-deps-for-path
  "Starting from an absolute path, traverse dependency relationships, updating
   the dependency graph. Returns a channel that closes when the process is
   done, or yields an error and then closes if the process fails."
  [;; Injected dependencies
   deps read-file
   ;; Arguments
   graph config path]
  (->> (file/from-path read-file path)
       (a/flat-map #(deps % config))
       (a/then
        (fn [dependencies]
          (doseq [dependency dependencies]
            (swap! graph dependency/depend path dependency))
          (->> dependencies
               (map #(add-deps-for-path deps read-file graph config %))
               (a/await-all 5000))))))

(defn- deps-graph
  "Gets a dependency graph of absolute paths based on the configuration map."
  [;; Injected dependencies
   logger exists? read-file deps
   ;; Arguments
   config build-id]
  (logging/info logger "Building dependency graph")
  (let [graph
        (atom (dependency/graph))

        ns-name->absolute-path
        (partial cljs/ns-name->absolute-path exists? (:source-paths config))

        add-deps
        (partial add-deps-for-path deps read-file graph config)]
    (->> (get-entries config build-id)
         (map ns-name->absolute-path)
         (a/await-all 5000)
         (a/flat-map #(->> %
                           (map add-deps)
                           (a/await-all 5000)))
         (a/map (fn [_] @graph)))))

;; Next: Create dependency graph


(defmethod ig/init-key ::deps-graph
  [_ {:keys [logger exists? read-file deps]}]
  (partial deps-graph logger exists? read-file deps))
