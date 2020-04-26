(ns css-gardener.core.cljs-parsing
  (:require ["path" :as path]
            [clojure.core.async :refer [go merge <!]]
            [clojure.string :as string]
            [clojure.tools.namespace.parse :as parse]
            [clojure.tools.reader.reader-types :refer [string-push-back-reader]]
            [css-gardener.core.utils.async :as a]
            [css-gardener.core.utils.errors :as errors]))

;; This is a copy of part of the clojure.tools.namespace.parse
;; namespace. Instead of copying this, fork the repo and publish my own
;; fork on Clojars.
;; 
;; https://github.com/clojure/tools.namespace
;; https://github.com/clojure/tools.namespace/blob/master/src/main/clojure/clojure/tools/namespace/parse.cljc#L61

;;; Parsing dependencies

(defn- prefix-spec?
  "Returns true if form represents a libspec prefix list like
  (prefix name1 name1) or [com.example.prefix [name1 :as name1]]"
  [form]
  (and (sequential? form)  ; should be a list, but often is not
       (symbol? (first form))
       (not-any? keyword? form)
       (< 1 (count form))))  ; not a bare vector like [foo]

(defn- option-spec?
  "Returns true if form represents a libspec vector containing optional
  keyword arguments like [namespace :as alias] or
  [namespace :refer (x y)] or just [namespace]"
  [form]
  (and (sequential? form)  ; should be a vector, but often is not
       (or (symbol? (first form)) ;; Local change (handling string namespaces)
           (string? (first form)))
       (or (keyword? (second form))  ; vector like [foo :as f]
           (= 1 (count form)))))  ; bare vector like [foo]

(defn- deps-from-libspec [prefix form]
  (cond (prefix-spec? form)
        (mapcat (fn [f] (deps-from-libspec
                         (symbol (str (when prefix (str prefix "."))
                                      (first form)))
                         f))
                (rest form))
        (option-spec? form)
        (deps-from-libspec prefix (first form))
        (symbol? form)
        (list (symbol (str (when prefix (str prefix ".")) form)))
        (keyword? form)  ; Some people write (:require ... :reload-all)
        nil
        (string? form) ;; Local change (handling string namespaces)
        (list form)
        :else
        (throw (ex-info "Unparsable namespace form"
                        {:reason ::unparsable-ns-form
                         :form form}))))

(def ^:private ns-clause-head-names
  "Set of symbol/keyword names which can appear as the head of a
  clause in the ns form."
  #{"use" "require"})

(def ^:private ns-clause-heads
  "Set of all symbols and keywords which can appear at the head of a
  dependency clause in the ns form."
  (set (mapcat (fn [name] (list (keyword name)
                                (symbol name)))
               ns-clause-head-names)))

(defn- deps-from-ns-form [form]
  (when (and (sequential? form)  ; should be list but sometimes is not
             (contains? ns-clause-heads (first form)))
    (mapcat #(deps-from-libspec nil %) (rest form))))

(defn deps-from-ns-decl
  "Given an (ns...) declaration form (unevaluated), returns a set of
  symbols naming the dependencies of that namespace.  Handles :use and
  :require clauses but not :load."
  [decl]
  (set (mapcat deps-from-ns-form decl)))

;; End forked code from clojure.tools.namespace.parse

(defn ns-name->relative-path
  [ns-name]
  (if (string? ns-name)
    ns-name
    (-> (str ns-name)
        (string/replace #"\." path/sep)
        (string/replace #"-" "_"))))

(def ^:private cljs-file-extensions #{".cljs" ".cljc"})

(defn ns-name->possible-absolute-paths
  [ns-name source-paths]
  (set (for [source-path source-paths
             extension cljs-file-extensions]
         (let [filename (str (ns-name->relative-path ns-name) extension)]
           (path/resolve source-path filename)))))

(defn ns-name->absolute-path
  "Gets the absolute path to the cljs file in the current project matching the
  namespace name.

  Args:
    ns-name: symbol or string ns name
    source-paths: paths relative to the project root to search for the file
    exists?: Function that returns a core async channel with a boolean
      indicating whether or not a file exists."
  [ns-name source-paths exists?]
  (go
    (let [existing-files (->> (ns-name->possible-absolute-paths
                               ns-name source-paths)
                              (map #(go {:path % :exists? (<! (exists? %))}))
                              merge
                              (a/take-all 5000)
                              <!
                              (filter :exists?)
                              (map :path))]
      (case (count existing-files)
        0 (errors/not-found (js/Error. (str "No file matching namespace "
                                            ns-name)))
        1 (first existing-files)
        (errors/conflict (str "More than 1 file found matching namespace "
                              ns-name
                              ": "
                              existing-files))))))

(defn cljs-deps-from-ns-decl
  [ns-decl source-paths exists?]
  (->> (deps-from-ns-decl ns-decl)
       (map #(ns-name->absolute-path % source-paths exists?))
       merge
       (a/take-all 5000)))

(defn- stylesheet-deps-relative-paths
  [ns-decl]
  (-> (parse/name-from-ns-decl ns-decl)
      meta
      :css-gardener/require
      set))

(defn stylesheet-deps-from-ns-decl
  [ns-decl current-file]
  (->> (stylesheet-deps-relative-paths ns-decl)
       (map #(path/resolve (path/dirname current-file) %))
       set))

(defn- all-deps-from-ns-decl
  [ns-decl current-file source-paths exists?]
  (->> (cljs-deps-from-ns-decl ns-decl source-paths exists?)
       (a/map #(into (stylesheet-deps-from-ns-decl ns-decl current-file) %))))

(defn- read-ns-decl
  [content]
  (parse/read-ns-decl (string-push-back-reader content)))

(defn all-deps-from-file
  [{:keys [absolute-path content]} source-paths exists?]
  (let [ns-decl (read-ns-decl content)]
    (all-deps-from-ns-decl ns-decl absolute-path source-paths exists?)))