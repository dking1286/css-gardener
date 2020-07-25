(ns css-gardener.core.transformation-test
  (:require [clojure.core.async :refer [<!]]
            [clojure.spec.alpha :as s]
            [clojure.string :as string]
            [clojure.test :refer [deftest is use-fixtures]]
            [clojure.tools.namespace.dependency :as ctnd]
            [css-gardener.core.caching :as caching]
            [css-gardener.core.config :as config]
            [css-gardener.core.dependency :as dependency]
            [css-gardener.core.logging :as logging]
            [css-gardener.core.modules :as modules]
            [css-gardener.core.system :as system]
            [css-gardener.core.transformation :as transformation]
            [css-gardener.core.utils.errors :as errors]
            [css-gardener.core.utils.fs :as fs]
            [css-gardener.core.utils.testing :refer [deftest-async
                                                     testing
                                                     with-system
                                                     instrument-specs]]
            [goog.object :as gobj]
            [integrant.core :as ig]
            [path]))

(use-fixtures :each instrument-specs)

(def ^:private cwd (path/resolve "."))

(defn- src-file
  [relative-path]
  (str cwd "/src/" relative-path))

(def ^:private files
  {(src-file "hello/world.cljs") "Hello world"
   (src-file "some/namespace.cljs") "Some namespace"
   (src-file "some/other/namespace.cljs") "Some other namespace"
   (src-file "some/third/namespace.cljs") "Some third namespace"
   (src-file "foo/foo.cljs") "Foo"
   (src-file "foo/bar.cljs") "Bar"
   (src-file "foo/bar.scss") "Bar styles"
   (src-file "foo/baz.cljs") "Baz"
   (src-file "foo/baz.scss") "Baz styles"
   (src-file "foo/bang.scss") "Bang styles"})

(def ^:private dependencies
  {(src-file "some/namespace.cljs") #{(src-file "foo/foo.cljs")}
   (src-file "foo/foo.cljs") #{(src-file "foo/bar.cljs")
                               (src-file "foo/baz.cljs")}
   (src-file "foo/bar.cljs") #{(src-file "foo/baz.cljs")
                               (src-file "foo/bar.scss")}
   (src-file "foo/baz.cljs") #{(src-file "foo/baz.scss")}
   (src-file "foo/bar.scss") #{(src-file "foo/bang.scss")}
   (src-file "foo/bang.scss") #{}})

(def ^:private dependency-graph
  (->> dependencies
       (mapcat (fn [[file deps]]
                 (map #(vector file %) deps)))
       (reduce (fn [graph [file dep]]
                 (ctnd/depend graph file dep))
               (ctnd/graph))))

(def ^:private no-styles-dependency-graph
  (->> dependencies
       (mapcat (fn [[file deps]]
                 (map #(vector file %) deps)))
       (filter (fn [[file dep]]
                 (not (or (string/ends-with? file ".scss")
                          (string/ends-with? dep ".scss")))))
       (reduce (fn [graph [file dep]]
                 (ctnd/depend graph file dep))
               (ctnd/graph))))

(def ^:private modules
  {{:node-module "@css-gardener/scope-transformer"}
   (transformation/transformer-stub
    nil {:absolute-path "" :content ""})

   {:node-module "@css-gardener/sass-transformer"}
   (transformation/transformer-stub
    nil {:absolute-path "" :content ""})

   {:node-module "@css-gardener/postcss-transformer"}
   (transformation/transformer-stub
    nil {:absolute-path "" :content ""})

   {:node-module "@css-gardener/sass-resolver"}
   (dependency/resolver-stub nil #js [])})

(def ^:private config
  {:source-paths ["src"]

   :builds
   {:app {:output-dir "public/css"
          :modules {:main {:entries ['some.namespace]}
                    :second {:entries ['some.other.namespace
                                       'some.third.namespace]
                             :depends-on #{:main}}}}}
   :rules
   {".css"
    {:transformers [{:node-module "@css-gardener/scope-transformer"}]}

    ".scss"
    {:dependency-resolver {:node-module "@css-gardener/sass-resolver"}
     :transformers [{:node-module "@css-gardener/scope-transformer"}
                    {:node-module "@css-gardener/sass-transformer"}]}

    ".sass"
    {:dependency-resolver {:node-module "@css-gardener/sass-resolver"}
     :transformers [{:node-module "@css-gardener/sass-transformer"
                     :options {:use-indented-syntax true}}]}}

   :postprocessing
   {:transformers [{:node-module "@css-gardener/postcss-transformer"}]}})

(def ^:private sys-config
  (-> system/config
      (assoc ::config/config config)
      (assoc-in [::fs/exists? :files] files)
      (assoc-in [::fs/read-file :files] files)
      (assoc-in [::modules/load :modules] modules)
      (assoc-in [::logging/logger :level] :debug)
      (assoc-in [::logging/logger :sinks] #{:cache})))

(deftest t-transformer
  (testing "returns false if the input is nil"
    (is (false? (transformation/transformer? nil))))
  (testing "returns false if a value does not have a 'enter' property"
    (is (false? (transformation/transformer? #js {:exit identity}))))
  (testing "returns false if a value does not have an 'exit' property"
    (is (false? (transformation/transformer? #js {:enter identity}))))
  (testing "returns true if a value has 'enter' and 'exit' properties"
    (is (true? (transformation/transformer? #js {:enter identity
                                                 :exit identity})))))

(deftest t-transformers
  (testing "throws an error at system initialization if one of the transformers
            specified in the config is not found"
    (let [sys-config
          (-> sys-config
              (update-in [::modules/load :modules]
                         dissoc {:node-module "@css-gardener/sass-transformer"}))]
      (try
        (ig/init sys-config)
        (throw (js/Error. "fail"))
        (catch js/Error err
          (is (string/includes? (errors/message err)
                                "Error on key :css-gardener.core.transformation/transformers"))))))
  (testing "Is a map from module names to the loaded transformers"
    (with-system [system sys-config]
      (let [transformers (::transformation/transformers system)]
        (is (= 3 (count transformers)))
        (is (s/valid? ::transformation/transformer-config
                      (get transformers
                           {:node-module "@css-gardener/sass-transformer"})))
        (is (s/valid? ::transformation/transformer-config
                      (get transformers
                           {:node-module "@css-gardener/postcss-transformer"})))))))

(def ^:private fake-scope-transformer
  #js {:enter (fn [file _ callback]
                (let [result (gobj/clone file)]
                  (gobj/set result "scopeEnter" "blah")
                  (callback nil result)))

       :exit (fn [file _ callback]
               (let [result (gobj/clone file)]
                 (gobj/set result "scopeExit" "blah")
                 (callback nil result)))})

(def ^:private fake-sass-transformer
  #js {:enter (fn [file _ callback]
                (let [result (gobj/clone file)]
                  (gobj/set result
                            "content" (str "Transformed by sass-transformer: "
                                           (gobj/get file "content")))
                  (callback nil result)))

       :exit (fn [file _ callback]
               (callback nil file))})

(def ^:private fake-postcss-transformer
  #js {:enter (fn [file _ callback]
                (let [result (gobj/clone file)]
                  (gobj/set result
                            "content" (str "Transformed by postcss-transformer: "
                                           (gobj/get file "content")))
                  (callback nil result)))

       :exit (fn [file _ callback]
               (callback nil file))})

(def ^:private fake-erroring-sass-transformer
  #js {:enter (fn [_ _ callback]
                (callback (js/Error. "boom") nil))

       :exit (fn [file _ callback]
               (callback nil file))})

(deftest-async t-transform
  (testing "yields invalid-config when there is no matching rule for the file"
    (with-system [system sys-config]
      (let [transform (::transformation/transform system)
            file {:absolute-path "/path/to/file.blah"
                  :content ""}]
        (is (errors/invalid-config? (<! (transform file)))))))
  (testing "yields unexpected-error if one of the transformers yields an error"
    (let [sys-config
          (-> sys-config
              (update-in [::modules/load :modules] assoc
                         {:node-module "@css-gardener/sass-transformer"}
                         fake-erroring-sass-transformer))]
      (with-system [system sys-config]
        (let [transform (::transformation/transform system)
              file {:absolute-path "/path/to/file.scss"
                    :content "hello world"}]
          (is (errors/unexpected-error? (<! (transform file))))))))
  (testing "yields the transformed file if none of the transformers yields an
            error"
    (let [sys-config
          (-> sys-config
              (update-in [::modules/load :modules] assoc
                         {:node-module "@css-gardener/sass-transformer"}
                         fake-sass-transformer
                         {:node-module "@css-gardener/scope-transformer"}
                         fake-scope-transformer))]
      (with-system [system sys-config]
        (let [transform (::transformation/transform system)
              file {:absolute-path "/path/to/file.scss"
                    :content "hello world"}]
          (is (= {:absolute-path "/path/to/file.scss"
                  :content "Transformed by sass-transformer: hello world"
                  :scope-enter "blah"
                  :scope-exit "blah"}
                 (<! (transform file)))))))))

(deftest-async t-compile-all
  (testing "yields an empty collection when there are no style files in the
            dependency graph"
    (let [sys-config
          (-> sys-config
              (update-in [::modules/load :modules] assoc
                         {:node-module "@css-gardener/sass-transformer"}
                         fake-sass-transformer
                         {:node-module "@css-gardener/scope-transformer"}
                         fake-scope-transformer
                         {:node-module "@css-gardener/postcss-transformer"}
                         fake-postcss-transformer))]
      (with-system [system sys-config]
        (let [compile-all (::transformation/compile-all system)]
          (is (= [] (<! (compile-all :app no-styles-dependency-graph))))))))
  (testing "yields a file that concatenates the contents of the transformed
            stylesheets, but only the root-level stylesheets, then transformed
            by the postprocessor transformers."
    (let [sys-config
          (-> sys-config
              (update-in [::modules/load :modules] assoc
                         {:node-module "@css-gardener/sass-transformer"}
                         fake-sass-transformer
                         {:node-module "@css-gardener/scope-transformer"}
                         fake-scope-transformer
                         {:node-module "@css-gardener/postcss-transformer"}
                         fake-postcss-transformer))]
      (with-system [system sys-config]
        (let [compile-all (::transformation/compile-all system)]
          (is (= [{:absolute-path (str cwd "/public/css/main.css")
                   :content "Transformed by postcss-transformer: Transformed by sass-transformer: Bar styles\n\nTransformed by sass-transformer: Baz styles"}]
                 (<! (compile-all :app dependency-graph))))))))
  (testing "sets the transformed value in the cache if it doesn't already exist"
    (let [sys-config
          (-> sys-config
              (update-in [::modules/load :modules] assoc
                         {:node-module "@css-gardener/sass-transformer"}
                         fake-sass-transformer
                         {:node-module "@css-gardener/scope-transformer"}
                         fake-scope-transformer
                         {:node-module "@css-gardener/postcss-transformer"}
                         fake-postcss-transformer))]
      (with-system [system sys-config]
        (let [cache (::caching/compilation-cache system)
              compile-all (::transformation/compile-all system)
              src-file-path (src-file "foo/bar.scss")]
          (is (not (caching/found? (<! (caching/get cache src-file-path)))))
          (<! (compile-all :app dependency-graph))
          (is (= {:absolute-path src-file-path
                  :content "Transformed by sass-transformer: Bar styles"}
                 (-> (caching/get cache src-file-path)
                     <!
                     (select-keys [:absolute-path :content])))))))))
