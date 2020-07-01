(ns css-gardener.core.dependency
  (:require [clojure.core.async :refer [go]]
            [clojure.spec.alpha :as s]
            [clojure.tools.namespace.dependency :as dependency]
            [css-gardener.core.cljs-parsing :as cljs]
            [css-gardener.core.config :as config]
            [css-gardener.core.file :as file]
            [css-gardener.core.logging :as logging]
            [css-gardener.core.modules :as modules]
            [css-gardener.core.utils.async :as a]
            [css-gardener.core.utils.errors :as errors]
            [css-gardener.core.utils.js :refer [to-js]]
            [css-gardener.core.utils.spec :as su]
            [integrant.core :as ig]))

;; ::resolvers ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(s/def ::options map?)
(s/def ::function fn?)
(s/def ::resolver
  (s/keys :req-un [::modules/module ::function]))

(defn resolver-stub
  "Creates a stub dependency resolver function for use in tests."
  [err result]
  (fn [_ _ callback]
    ;; Call the callback asynchronously, just so that there's no hidden
    ;; assumption about it being synchronous in a test.
    (go (callback err result))))

(defmethod ig/init-key ::resolvers
  [_ {:keys [logger load-module config]}]
  (logging/debug logger "Loading dependency resolvers")
  (->> (:rules config)
       vals
       (map :dependency-resolver)
       (filter (complement nil?))
       (map (fn [config]
              {:module (modules/extract-module config)
               :function (load-module (modules/extract-module config))}))
       (map #(vector (:module %) %))
       (into {})))

;; ::deps ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(s/fdef resolve-deps
  :args (s/cat :resolver ::resolver
               :options ::options
               :file ::file/file))

(defn- resolve-deps
  [{:keys [module function]} options file]
  (a/node-callback->channel
   function
   (to-js file)
   (to-js options)
   (fn [err deps]
     (cond
       err err

       (js/Array.isArray deps) deps

       :else
       (errors/invalid-dependency-resolver
        (str "Expected dependency resolver " module
             " to yield an array of strings, got "
             deps))))))

(s/fdef deps
  :args (s/cat :config ::config/config
               :logger ::logging/logger
               :resolvers (s/map-of ::modules/module ::resolver)
               :cljs-deps fn?
               :file ::file/file))

(defn- deps
  [;; Injected dependencies
   config logger resolvers cljs-deps
   ;; Arguments
   file]
  (logging/debug logger (str "Getting dependencies of "
                             (:absolute-path file)))
  (let [rule-or-error (config/matching-rule config (:absolute-path file))]
    (cond
      (cljs/cljs-file? (:absolute-path file))
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
      (let [resolver-module (some-> rule-or-error
                                    :dependency-resolver
                                    modules/extract-module)
            resolver (get resolvers resolver-module)
            options (or (get-in rule-or-error [:dependency-resolver :options])
                        {})]
        (->> (resolve-deps resolver options file)
             (a/map set))))))

(defmethod ig/init-key ::deps
  [_ {:keys [config logger resolvers cljs-deps fake-dependencies error]}]
  (cond
    ;; Mock deps with hard-coded map of dependencies
    fake-dependencies (fn [file _]
                        (go (or (get fake-dependencies (:absolute-path file))
                                #{})))
    ;; Mock deps that yields an error
    error (fn [_ _] (go error))
    ;; Real deps implementation
    :else (partial deps config logger resolvers cljs-deps)))

;; ::deps-graph ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(s/def ::dependency-graph #(and (satisfies? dependency/DependencyGraph %)
                                (satisfies? dependency/DependencyGraphUpdate %)))

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
   config logger exists? read-file deps
   ;; Arguments
   build-id & {:keys [initial-graph
                      entry-files]
               :or {initial-graph (dependency/graph)}}]
  (logging/info logger "Building dependency graph")
  (let [graph
        (atom initial-graph)

        ns-name->absolute-path
        (partial cljs/ns-name->absolute-path exists? (:source-paths config))

        add-deps
        (partial add-deps-for-path deps read-file graph config)

        entries (or (and entry-files (go entry-files))
                    (->> (get-entries config build-id)
                         (map ns-name->absolute-path)
                         (a/await-all 5000)))]
    (->> entries
         (a/flat-map #(->> %
                           (map add-deps)
                           (a/await-all 5000)))
         (a/map (fn [_] @graph)))))

(defmethod ig/init-key ::deps-graph
  [_ {:keys [config logger exists? read-file deps]}]
  (partial deps-graph config logger exists? read-file deps))
