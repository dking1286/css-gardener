(ns css-gardener.core.transformation
  (:require [clojure.core.async :refer [go]]
            [clojure.pprint :refer [pprint]]
            [clojure.spec.alpha :as s]
            [clojure.string :as string]
            [clojure.tools.namespace.dependency :as ctnd]
            [css-gardener.core.caching :as caching]
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

(defn- load-transformer-specs
  [load-module specs]
  (->> specs
       (map (fn [config]
              (let [module (modules/extract-module config)]
                {:module module
                 :transformer (load-module module)})))
       (map #(vector (:module %) %))
       (into {})))

(defn- transformer-specs
  [config]
  (->> (:rules config)
       vals
       (mapcat :transformers)))

(defn- postprocessor-specs
  [config]
  (-> config :postprocessing :transformers))

(defmethod ig/init-key ::transformers
  [_ {:keys [config logger load-module]}]
  (logging/debug logger "Loading transformers")
  (->> (concat (transformer-specs config)
               (postprocessor-specs config))
       (load-transformer-specs load-module)))

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

(defn- enabled?
  [module mode]
  (mode (meta module)))

(defn- apply-transformer-function
  [{:keys [function options module]} mode file]
  (if-not (enabled? module mode)
    (go file)
    (a/callback->channel
     function
     file
     (to-js options)
     (fn [err new-file]
       (if err
         (errors/unexpected-error (str "Error in transformer " module)
                                  err)
         new-file)))))

(defn- apply-transformer-stack
  [transformers-stack mode file]
  (let [functions (get-transformer-functions transformers-stack)]
    (loop [result (go (to-js file))
           remaining-functions functions]
      (if (empty? remaining-functions)
        (a/map from-js result)
        (let [func (first remaining-functions)]
          (recur (a/flat-map #(apply-transformer-function func mode %) result)
                 (rest remaining-functions)))))))

(defn- transform
  "Transforms a file using the transformers specified in the config.
   
   Each transformer's :enter method is called in order, and then each
   transformer's :exit method is called in reverse order."
  [{:keys [config mode transformers]} file]
  (let [rule-or-error (config/matching-rule config (:absolute-path file))]
    (if (errors/error? rule-or-error)
      (go (errors/invalid-config (str "Problem finding rule for file "
                                      (:absolute-path file))
                                 rule-or-error))
      (let [transformers-stack (get-transformers-stack
                                transformers (:transformers rule-or-error))]
        (apply-transformer-stack transformers-stack mode file)))))

(defmethod ig/init-key ::transform
  [_ dependencies]
  (partial transform dependencies))

(defn- postprocess
  "Transforms an output file using the postprocessing transformers specified in
   this config.
   
   Each transformer's :enter method is called in order, and then each
   transformer's :exit method is called in reverse order."
  [{:keys [config mode transformers]} file]
  (let [transformers-stack (get-transformers-stack
                            transformers
                            (-> config :postprocessing :transformers))]
    (apply-transformer-stack transformers-stack mode file)))

(defmethod ig/init-key ::postprocess
  [_ dependencies]
  (partial postprocess dependencies))

;; :compile-all ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

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

(defn style-file?
  "Determines if a file is a style file."
  [config absolute-path]
  (and (not (cljs/cljs-file? absolute-path))
       (not (false? (:style? (config/matching-rule config absolute-path))))))

(defn root-style-file?
  "Determines if a file is a style file that is not depended on by any other
   style files."
  [config dependency-graph absolute-path]
  (and (style-file? config absolute-path)
       (empty? (->> (ctnd/immediate-dependents dependency-graph absolute-path)
                    (filter #(style-file? config %))))))

(defn get-root-style
  "Gets the root style(s) associated with the passed-in absolute path"
  [config dependency-graph absolute-path]
  (->> (ctnd/transitive-dependents dependency-graph
                                   absolute-path)
       (filter #(root-style-file? config dependency-graph %))))

(defn- get-root-styles
  "Gets a set of all root style files in the dependency graph."
  [config dependency-graph]
  (->> (ctnd/nodes dependency-graph)
       (filter #(root-style-file? config dependency-graph %))))

(defn- compile-file
  [compilation-cache read-file transform absolute-path]
  (caching/with-cache compilation-cache absolute-path
    (->> (file/from-path read-file absolute-path)
         (a/flat-map transform))))

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
   {:keys [logger config compilation-cache read-file transform postprocess]}
   ;; Arguments
   build-id dependency-graph]
  (logging/debug logger (str "Compiling styles from dependency graph "
                             (with-out-str (pprint dependency-graph))))
  (let [output-dir (get-in config [:builds build-id :output-dir])
        root-styles (get-root-styles config dependency-graph)]
    (logging/debug logger (str "Found root style files: "
                               (with-out-str (pprint root-styles))))
    (->> root-styles
         (map #(compile-file compilation-cache read-file transform %))
         (a/await-all 10000)
         (a/map #(create-output-files config build-id output-dir %))
         (a/flat-map #(->> %
                           (map postprocess)
                           (a/await-all 10000))))))

(defmethod ig/init-key ::compile-all
  [_ dependencies]
  (partial compile-all dependencies))