(ns css-gardener.core.transformation
  (:require [clojure.core.async :refer [go]]
            [clojure.spec.alpha :as s]
            [clojure.string :as string]
            [clojure.tools.namespace.dependency :as ctnd]
            [css-gardener.core.cljs-parsing :as cljs]
            [css-gardener.core.config :as config]
            [css-gardener.core.file :as file]
            [css-gardener.core.logging :as logging]
            [css-gardener.core.modules :as modules]
            [css-gardener.core.utils.async :as a]
            [css-gardener.core.utils.errors :as errors]
            [css-gardener.core.utils.js :refer [to-js from-js]]
            [goog.object :as gobj]
            [integrant.core :as ig]
            [path]))

;; ::transformers ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn transformer?
  "Determines whether or not a value is a valid transformer."
  [val]
  (boolean (and (gobj/get val "enter")
                (gobj/get val "exit"))))

(s/def ::transformer transformer?)
(s/def ::options map?)
(s/def ::transformer-config
  (s/keys :req-un [::modules/module ::transformer]))

(defn transformer-stub
  "Creates a stub transformer for use in tests."
  [err result]
  #js {:enter (fn [_ _ callback]
                (go (callback err result)))
       :exit (fn [_ _ callback]
               (go (callback err result)))})

(defmethod ig/init-key ::transformers
  [_ {:keys [config logger load-module]}]
  (logging/debug logger "Loading transformers")
  (->> (:rules config)
       vals
       (mapcat :transformers)
       (map (fn [config]
              (let [module (modules/extract-module config)]
                {:module module
                 :transformer (load-module module)})))
       (map #(vector (:module %) %))
       (into {})))

;; ::transform ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- get-transformers-stack
  [transformers transformers-rule]
  (->> transformers-rule
       (map #(assoc (get transformers (modules/extract-module %))
                    :options (:options %)))))

(defn- get-transformer-functions
  [transformers-stack]
  (concat (map #(assoc % :function (gobj/get (:transformer %) "enter"))
               transformers-stack)
          (reverse (map #(assoc % :function (gobj/get (:transformer %) "exit"))
                        transformers-stack))))

(defn- apply-transformer-function
  [{:keys [function options module]} file]
  (a/callback->channel
   function
   file
   (to-js options)
   (fn [err new-file]
     (if err
       (errors/unexpected-error (str "Error in transformer " module)
                                err)
       new-file))))

(s/fdef transform
  :args (s/cat :config ::config/config
               :transformers (s/map-of ::modules/module ::transformer-config)
               :file ::file/file))

(defn- transform
  "Transforms a file using the transformers specified in the config.
   
   Each transformer's :enter method is called in order, and then each
   transformer's :exit method is called in reverse order."
  [;; Injected dependencies
   config transformers
   ;; Arguments
   file]
  (let [rule-or-error (config/matching-rule config (:absolute-path file))]
    (if (errors/error? rule-or-error)
      (go (errors/invalid-config (str "Problem finding rule for file "
                                      (:absolute-path file))
                                 rule-or-error))
      (let [transformers-stack (get-transformers-stack
                                transformers (:transformers rule-or-error))
            functions (get-transformer-functions transformers-stack)]
        (loop [result (go (to-js file))
               remaining-functions functions]
          (if (empty? remaining-functions)
            (a/map from-js result)
            (let [func (first remaining-functions)]
              (recur (a/flat-map #(apply-transformer-function func %) result)
                     (rest remaining-functions)))))))))

(defmethod ig/init-key ::transform
  [_ {:keys [config transformers]}]
  (partial transform config transformers))

;; :compile-all ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; TODO: Upgrade the compile-all function to support code-splitting. Right now,
;; we're just combining everything into a single output file.

(defn- get-top-level-module
  [config build-id]
  (->> (get-in config [:builds build-id :modules])
       (filter (fn [[_ value]] (empty? (:depends-on value))))
       (map first)
       sort
       first))

(defn- get-output-path
  [config build-id output-dir]
  (let [top-module-name (name (get-top-level-module config build-id))]
    (path/resolve "." output-dir (str  top-module-name ".css"))))

(defn- style-file?
  "Determines if a file is a style file."
  [config absolute-path]
  (and (not (cljs/cljs-file? absolute-path))
       (not (false? (:style? (config/matching-rule config absolute-path))))))

(defn- root-style-file?
  "Determines if a file is a style file that is not depended on by any other
   style files."
  [config dependency-graph absolute-path]
  (and (style-file? config absolute-path)
       (empty? (->> (ctnd/immediate-dependents dependency-graph absolute-path)
                    (filter #(style-file? config %))))))

(defn- get-root-styles
  "Gets a set of all root style files in the dependency graph."
  [config dependency-graph]
  (->> (ctnd/nodes dependency-graph)
       (filter #(root-style-file? config dependency-graph %))))

(defn- compile-file
  [read-file transform absolute-path]
  (->> (file/from-path read-file absolute-path)
       (a/flat-map transform)))

(defn- create-output-files
  [config build-id output-dir transformed-files]
  (let [output-path (get-output-path config build-id output-dir)]
    (if (empty? transformed-files)
      []
      [{:absolute-path output-path
        :content (->> transformed-files
                      (map :content)
                      (string/join "\n\n"))}])))

(defn- compile-all
  "Compiles all of the style files in the dependency graph into a collection
   of output files to be written."
  [;; Injected dependencies
   config read-file transform
   ;; Arguments
   build-id dependency-graph]
  (let [output-dir (get-in config [:builds build-id :output-dir])]
    (->> (get-root-styles config dependency-graph)
         (map #(compile-file read-file transform %))
         (a/await-all 10000)
         (a/map #(create-output-files config build-id output-dir %)))))

(defmethod ig/init-key ::compile-all
  [_ {:keys [config read-file transform]}]
  (partial compile-all config read-file transform))