(ns css-gardener.core.dependency-test
  (:require [clojure.core.async :refer [<!]]
            [clojure.string :as string]
            [clojure.test :refer [deftest is are use-fixtures]]
            [clojure.tools.namespace.dependency :as ctnd]
            [css-gardener.core.config :as config]
            [css-gardener.core.dependency :as dependency]
            [css-gardener.core.logging :as logging]
            [css-gardener.core.modules :as modules]
            [css-gardener.core.system :as system]
            [css-gardener.core.utils.errors :as errors]
            [css-gardener.core.utils.fs :as fs]
            [css-gardener.core.utils.testing :refer [testing
                                                     instrument-specs
                                                     with-system
                                                     deftest-async]]
            [goog.object :as gobj]
            [integrant.core :as ig]
            [path]))

(use-fixtures :once instrument-specs)

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
   (src-file "foo/bang.scss") "Bang styles"})

(def ^:private dependencies
  {(src-file "some/namespace.cljs") #{(src-file "foo/foo.cljs")}
   (src-file "foo/foo.cljs") #{(src-file "foo/bar.cljs")
                               (src-file "foo/baz.cljs")}
   (src-file "foo/bar.cljs") #{(src-file "foo/baz.cljs")
                               (src-file "foo/bar.scss")}
   (src-file "foo/baz.cljs") #{}
   (src-file "foo/bar.scss") #{(src-file "foo/bang.scss")}
   (src-file "foo/bang.scss") #{}})

(def ^:private circular-dependencies
  (update dependencies (src-file "foo/bar.cljs")
          conj (src-file "foo/foo.cljs")))

(def ^:private nonexistent-cljs-dependencies
  (update dependencies (src-file "foo/bar.cljs")
          conj (src-file "foo/does_not_exist.cljs")))

(def ^:private nonexistent-style-dependencies
  (update dependencies (src-file "foo/bar.scss")
          conj (src-file "foo/does_not_exist.scss")))

(def ^:private config
  {:source-paths ["src"]
   :builds {:app {:target :browser
                  :output-dir "public/js"
                  :asset-path "/js"
                  :modules {:main {:entries ['some.namespace]}
                            :second {:entries ['some.other.namespace
                                               'some.third.namespace]
                                     :depends-on #{:main}}}}}
   :rules
   {".css" {:transformers []}
    ".scss" {:dependency-resolver {:node-module "@css-gardener/sass-resolver"
                                   :options {:use-indented-syntax true}}
             :transformers [{:node-module "@css-gardener/sass-transformer"}]}}})

(def ^:private modules
  {{:node-module "@css-gardener/sass-resolver"}
   (dependency/resolver-stub nil #js [])

   {:node-module "@css-gardener/sass-transformer"}
   (fn [file cb] (cb nil file))})

(def ^:private sys-config
  (-> system/config
      (assoc ::config/config config)
      (assoc-in [::fs/exists? :files] files)
      (assoc-in [::fs/read-file :files] files)
      (assoc-in [::modules/load :modules] modules)
      (assoc-in [::logging/logger :level] :debug)
      (assoc-in [::logging/logger :sinks] #{:cache})))

(def ^:private ns-decl
  '(ns hello.world
     (:require [some.other.namespace]
               [some.third.namespace])))

(deftest t-resolvers
  (testing "throws an error at system initialization if one of the resolvers
            specified in the config is not found"
    (let [sys-config
          (-> sys-config
              (update-in [::modules/load :modules]
                         dissoc {:node-module "@css-gardener/sass-resolver"}))]
      (try
        (ig/init sys-config)
        (throw (js/Error. "fail"))
        (catch js/Error err
          (is (string/includes? (errors/message err)
                                "Error on key :css-gardener.core.dependency/resolvers"))))))
  (testing "yields a map of dependency resolver names to loaded functions"
    (with-system [system sys-config]
      (let [resolvers (::dependency/resolvers system)]
        (is (= (get modules {:node-module "@css-gardener/sass-resolver"})
               (get-in resolvers [{:node-module "@css-gardener/sass-resolver"}
                                  :function])))))))

(deftest-async t-deps
  (testing "returns the cljs deps if the file is a cljs file"
    (with-system [system sys-config]
      (let [deps (::dependency/deps system)
            file {:absolute-path (src-file "hello/world.cljs")
                  :content (pr-str ns-decl)}]
        (is (= #{(src-file "some/other/namespace.cljs")
                 (src-file "some/third/namespace.cljs")}
               (<! (deps file)))))))
  (testing "skips a namespace dependency if no corresponding file is found,
            and logs a warning."
    (with-system [system
                  (-> sys-config
                      (update-in [::fs/exists? :files]
                                 dissoc
                                 (src-file "some/other/namespace.cljs")))]
      (let [deps (::dependency/deps system)
            logger (::logging/logger system)
            file {:absolute-path (src-file "hello/world.cljs")
                  :content (pr-str ns-decl)}]
        (is (= #{(src-file "some/third/namespace.cljs")}
               (<! (deps file))))
        (is (logging/has-message?
             logger
             #(string/includes? (.getMessage %)
                                "No file matching namespace some.other.namespace"))))))
  (testing "returns invalid-config if no rule matches the file"
    (with-system [system sys-config]
      (let [deps (::dependency/deps system)
            file {:absolute-path (src-file "hello/world.blah")
                  :content "blah"}]
        (is (errors/invalid-config? (<! (deps file)))))))
  (testing "returns invalid-config if multiple rules match the file"
    (with-system [system
                  (-> sys-config
                      (assoc-in [::config/config :rules "blah"]
                                {:transformers []})
                      (assoc-in [::config/config :rules "lah"]
                                {:transformers []}))]
      (let [deps (::dependency/deps system)
            file {:absolute-path (src-file "hello/world.blah")
                  :content "blah"}]
        (is (errors/invalid-config? (<! (deps file)))))))
  (testing "returns an empty set if the matching rule has no dependency
            resolver"
    (with-system [system
                  (-> sys-config
                      (assoc-in [::config/config :rules "blah"]
                                {:transformers []}))]
      (let [deps (::dependency/deps system)
            file {:absolute-path (src-file "hello/world.blah")
                  :content "blah"}]
        (is (= #{} (<! (deps file)))))))
  (testing "passes the file and options to the dependency resolver, as plain
            javascript objects with camelCase keys"
    (let [args (atom [])
          sys-config
          (-> sys-config
              (assoc-in [::modules/load
                         :modules
                         {:node-module "@css-gardener/sass-resolver"}]
                        (fn [file config callback]
                          (reset! args [file config])
                          (callback #js []))))]
      (with-system [system sys-config]
        (let [deps (::dependency/deps system)
              file {:absolute-path (src-file "hello/world.scss")
                    :content ""}]
          (<! (deps file))
          (is (gobj/equals #js {"absolutePath" (src-file "hello/world.scss")
                                "content" ""}
                           (first @args)))
          (is (gobj/equals #js {"useIndentedSyntax" true}
                           (second @args)))))))
  (testing "returns invalid-dependency-resolver if the dependency resolver
            yields a value that is not a javascript array"
    (let [sys-config
          (-> sys-config
              (assoc-in [::modules/load
                         :modules
                         {:node-module "@css-gardener/blah-resolver"}]
                        (dependency/resolver-stub
                         nil #{"/some/other/namespace.cljs"}))
              (assoc-in [::config/config :rules "blah"]
                        {:dependency-resolver {:node-module "@css-gardener/blah-resolver"}
                         :transformers []}))

          file
          {:absolute-path (src-file "hello/world.blah")
           :content "blah"}]

      (with-system [system sys-config]
        (let [deps (::dependency/deps system)
              result (<! (deps file))]
          (is (errors/invalid-dependency-resolver? result))
          (is (string/includes? (errors/message result)
                                (str "Expected dependency resolver "
                                     {:node-module "@css-gardener/blah-resolver"}
                                     " to yield an array of strings")))))))
  (testing "returns the value returned by the dependency resolver if one exists, coerced to a set"
    (let [sys-config
          (-> sys-config
              (assoc-in [::modules/load
                         :modules
                         {:node-module "@css-gardener/blah-resolver"}]
                        (dependency/resolver-stub
                         nil #js ["/some/other/namespace.cljs"]))
              (assoc-in [::config/config :rules "blah"]
                        {:dependency-resolver {:node-module "@css-gardener/blah-resolver"}
                         :transformers []}))

          file
          {:absolute-path (src-file "hello/world.blah")
           :content "blah"}]

      (with-system [system sys-config]
        (let [deps (::dependency/deps system)]
          (is (= #{"/some/other/namespace.cljs"}
                 (<! (deps file))))))))
  (testing "returns an unexpected-error if the dependency resolver gives an error"
    (let [sys-config
          (-> sys-config
              (assoc-in [::modules/load
                         :modules
                         {:node-module "@css-gardener/blah-resolver"}]
                        (dependency/resolver-stub
                         (js/Error. "Boom") nil))
              (assoc-in [::config/config :rules "blah"]
                        {:dependency-resolver {:node-module "@css-gardener/blah-resolver"}
                         :transformers []}))

          file
          {:absolute-path (src-file "hello/world.blah")
           :content "blah"}]
      (with-system [system sys-config]
        (let [deps (::dependency/deps system)]
          (is (errors/unexpected-error? (<! (deps file))))
          (is (= "Boom" (.-message (ex-cause (<! (deps file)))))))))))

(deftest t-get-entries
  (testing "Returns a set of all of the entry namespaces from the config"
    (is (= '#{some.namespace some.other.namespace some.third.namespace}
           (dependency/get-entries config :app)))))

(deftest-async t-deps-graph
  (testing "Logs a message at the info level"
    (with-system [system sys-config]
      (let [logger (::logging/logger system)
            deps-graph (::dependency/deps-graph system)]
        (deps-graph :app)
        (is (seq (->> @(:cache logger)
                      (filter #(= "Building dependency graph"
                                  (.getMessage %)))))))))
  (testing "Returns an error if there is a circular dependency"
    (with-system [system
                  (-> sys-config
                      (assoc-in [::dependency/deps :fake-dependencies]
                                circular-dependencies))]
      (let [deps-graph (::dependency/deps-graph system)
            result (<! (deps-graph :app))]
        (is (string/includes? (.-message result) "Circular dependency")))))
  (testing "Returns an error if a cljs file refers to a dependency that does not exist"
    (with-system [system
                  (-> sys-config
                      (assoc-in [::dependency/deps :fake-dependencies]
                                nonexistent-cljs-dependencies))]
      (let [deps-graph (::dependency/deps-graph system)
            result (<! (deps-graph :app))]
        (is (errors/not-found? result)))))
  (testing "Returns an error if a style file refers to a dependency that does not exist"
    (with-system [system
                  (-> sys-config
                      (assoc-in [::dependency/deps :fake-dependencies]
                                nonexistent-style-dependencies))]
      (let [deps-graph (::dependency/deps-graph system)
            result (<! (deps-graph :app))]
        (is (errors/not-found? result)))))
  (testing "Returns an error if the 'deps' function returns an error"
    (with-system [system
                  (-> sys-config
                      (assoc-in [::dependency/deps :error]
                                (errors/invalid-config "Boom")))]
      (let [deps-graph (::dependency/deps-graph system)
            result (<! (deps-graph :app))]
        (is (errors/invalid-config? result)))))
  (testing "Returns the dependency graph"
    (with-system [system
                  (-> sys-config
                      (assoc-in [::dependency/deps :fake-dependencies]
                                dependencies))]
      (let [deps-graph (::dependency/deps-graph system)
            result (<! (deps-graph :app))]
        (are [x y] (ctnd/depends? result x y)
          (src-file "some/namespace.cljs") (src-file "foo/foo.cljs")
          (src-file "some/namespace.cljs") (src-file "foo/baz.cljs")
          (src-file "some/namespace.cljs") (src-file "foo/bar.scss")
          (src-file "some/namespace.cljs") (src-file "foo/bang.scss"))
        (are [x y] (not (ctnd/depends? result x y))
          (src-file "foo/bang.scss") (src-file "some/namespace.cljs"))))))

(deftest t-really-remove-node
  (testing "Removes outgoing dependencies"
    (let [graph (-> (ctnd/graph)
                    (ctnd/depend "foo" "bar")
                    (ctnd/depend "bar" "baz")
                    (ctnd/depend "bar" "bang")
                    (dependency/really-remove-node "bar"))]
      (is (ctnd/depends? graph "foo" "bar"))
      (is (not (ctnd/depends? graph "bar" "baz")))
      (is (not (ctnd/depends? graph "bar" "bang")))))
  (testing "Removes dependents"
    (let [graph (-> (ctnd/graph)
                    (ctnd/depend "foo" "bar")
                    (ctnd/depend "bar" "baz")
                    (ctnd/depend "bar" "bang")
                    (dependency/really-remove-node "bar"))]
      (is (ctnd/dependent? graph "bar" "foo"))
      (is (not (ctnd/dependent? graph "baz" "bar")))
      (is (not (ctnd/dependent? graph "bang" "bar")))))
  (testing "Removes depended-on nodes entirely if they don't have any other
            dependents in the graph."
    (let [graph (-> (ctnd/graph)
                    (ctnd/depend "foo" "bar")
                    (ctnd/depend "bar" "baz")
                    (ctnd/depend "bar" "bang")
                    (dependency/really-remove-node "bar"))]
      (is (not (contains? (ctnd/nodes graph) "baz")))
      (is (not (contains? (ctnd/nodes graph) "bang")))))
  (testing "Does not remove depended-on nodes if they have other dependents
            in the graph."
    (let [graph (-> (ctnd/graph)
                    (ctnd/depend "foo" "baz")
                    (ctnd/depend "bar" "baz")
                    (dependency/really-remove-node "bar"))]
      (is (contains? (ctnd/nodes graph) "baz"))
      (is (ctnd/depends? graph "foo" "baz"))
      (is (ctnd/dependent? graph "baz" "foo"))
      (is (not (ctnd/dependent? graph "baz" "bar"))))))