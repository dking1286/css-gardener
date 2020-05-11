(ns css-gardener.core.dependency-test
  (:require [clojure.core.async :refer [<!]]
            [clojure.string :as string]
            [clojure.test :refer [deftest testing is are use-fixtures]]
            [clojure.tools.namespace.dependency :as ctnd]
            [css-gardener.core.dependency :as dependency]
            [css-gardener.core.logging :as logging]
            [css-gardener.core.modules :as modules]
            [css-gardener.core.system :as system]
            [css-gardener.core.utils.errors :as errors]
            [css-gardener.core.utils.fs :as fs]
            [css-gardener.core.utils.testing :refer [instrument-specs
                                                     with-system
                                                     deftest-async]]
            ["path" :as path]))

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
   {#"\.css$" {:transformers []}
    #"\.scss$" {:dependency-resolver {:node-module "@css-gardener/sass-resolver"}
                :transformers [{:node-module "@css-gardener/sass-transformer"}]}}})

(def ^:private sys-config
  (-> system/config
      (assoc-in [::fs/exists? :files] files)
      (assoc-in [::fs/read-file :files] files)
      (assoc-in [::modules/load :return-value] nil)
      (assoc-in [::logging/logger :level] :debug)
      (assoc-in [::logging/logger :sinks] #{:cache})))

(def ^:private ns-decl
  '(ns hello.world
     (:require [some.other.namespace]
               [some.third.namespace])))

(deftest-async t-deps
  (testing "returns the cljs deps if the file is a cljs file"
    (with-system [system sys-config]
      (let [deps (::dependency/deps system)
            file {:absolute-path (src-file "hello/world.cljs")
                  :content (pr-str ns-decl)}]
        (is (= #{(src-file "some/other/namespace.cljs")
                 (src-file "some/third/namespace.cljs")}
               (<! (deps file config)))))))
  (testing "returns invalid-config if no rule matches the file"
    (with-system [system sys-config]
      (let [deps (::dependency/deps system)
            file {:absolute-path (src-file "hello/world.blah")
                  :content "blah"}]
        (is (errors/invalid-config? (<! (deps file config)))))))
  (testing "returns invalid-config if multiple rules match the file"
    (with-system [system sys-config]
      (let [deps (::dependency/deps system)
            file {:absolute-path (src-file "hello/world.blah")
                  :content "blah"}
            config (-> config
                       (assoc-in [:rules #"blah$"] {:transformers []})
                       (assoc-in [:rules #"lah$"] {:transformers []}))]
        (is (errors/invalid-config? (<! (deps file config)))))))
  (testing "returns an empty set if the matching rule has no dependency resolver"
    (with-system [system sys-config]
      (let [deps (::dependency/deps system)
            file {:absolute-path (src-file "hello/world.blah")
                  :content "blah"}
            config (-> config
                       (assoc-in [:rules #"blah$"] {:transformers []}))]
        (is (= #{} (<! (deps file config)))))))
  (testing "returns invalid-dependency-resolver if load-module cannot find the dependency resolver"
    (let [file
          {:absolute-path (src-file "hello/world.blah")
           :content "blah"}

          config
          (-> config
              (assoc-in [:rules #"blah$"]
                        {;; Does not exist, will throw an error when trying to load the module
                         :dependency-resolver {:node-module "@css-gardener/blah-resolver"}
                         :transformers []}))]

      (with-system [system sys-config]
        (let [deps (::dependency/deps system)
              result  (<! (deps file config))]
          (is (errors/invalid-dependency-resolver? result))
          (is (string/includes? (.-message (ex-cause result))
                                "Cannot find module '@css-gardener/blah-resolver'"))))))
  (testing "returns invalid-dependency-resolver if the dependency resolver yields a value that is not a javascript array"
    (let [sys-config
          (-> sys-config
              (assoc-in [::modules/load :return-value]
                        (fn [_ cb] (cb nil #{"/some/other/namespace.cljs"}))))

          file
          {:absolute-path (src-file "hello/world.blah")
           :content "blah"}

          config
          (-> config
              (assoc-in [:rules #"blah$"]
                        {:dependency-resolver {:node-module "@css-gardener/blah-resolver"}
                         :transformers []}))]

      (with-system [system sys-config]
        (let [deps (::dependency/deps system)]
          (is (errors/invalid-dependency-resolver? (<! (deps file config))))))))
  (testing "returns the value returned by the dependency resolver if one exists, coerced to a set"
    (let [sys-config
          (-> sys-config
              (assoc-in [::modules/load :return-value]
                        (fn [_ cb] (cb nil #js ["/some/other/namespace.cljs"]))))

          file
          {:absolute-path (src-file "hello/world.blah")
           :content "blah"}

          config
          (-> config
              (assoc-in [:rules #"blah$"]
                        {:dependency-resolver {:node-module "@css-gardener/blah-resolver"}
                         :transformers []}))]

      (with-system [system sys-config]
        (let [deps (::dependency/deps system)]
          (is (= #{"/some/other/namespace.cljs"}
                 (<! (deps file config))))))))
  (testing "returns an unexpected-error if the dependency resolver gives an error"
    (let [sys-config
          (-> sys-config
              (assoc-in [::modules/load :return-value]
                        (fn [_ cb] (cb (js/Error. "Boom") nil))))

          file
          {:absolute-path (src-file "hello/world.blah")
           :content "blah"}

          config
          (-> config
              (assoc-in [:rules #"blah$"]
                        {:dependency-resolver {:node-module "@css-gardener/blah-resolver"}
                         :transformers []}))]
      (with-system [system sys-config]
        (let [deps (::dependency/deps system)]
          (is (errors/unexpected-error? (<! (deps file config))))
          (is (= "Boom" (.-message (ex-cause (<! (deps file config)))))))))))

(deftest t-get-entries
  (testing "Returns a set of all of the entry namespaces from the config"
    (is (= '#{some.namespace some.other.namespace some.third.namespace}
           (dependency/get-entries config :app)))))

(deftest-async t-deps-graph
  (testing "Logs a message at the info level"
    (with-system [system sys-config]
      (let [logger (::logging/logger system)
            deps-graph (::dependency/deps-graph system)]
        (deps-graph {} :app)
        (is (seq (->> @(:cache logger)
                      (filter #(= "Building dependency graph"
                                  (.getMessage %)))))))))
  (testing "Returns an error if there is a circular dependency"
    (with-system [system
                  (-> sys-config
                      (assoc-in [::dependency/deps :fake-dependencies]
                                circular-dependencies))]
      (let [deps-graph (::dependency/deps-graph system)
            result (<! (deps-graph config :app))]
        (is (string/includes? (.-message result) "Circular dependency")))))
  (testing "Returns an error if a cljs file refers to a dependency that does not exist"
    (with-system [system
                  (-> sys-config
                      (assoc-in [::dependency/deps :fake-dependencies]
                                nonexistent-cljs-dependencies))]
      (let [deps-graph (::dependency/deps-graph system)
            result (<! (deps-graph config :app))]
        (is (errors/not-found? result)))))
  (testing "Returns an error if a style file refers to a dependency that does not exist"
    (with-system [system
                  (-> sys-config
                      (assoc-in [::dependency/deps :fake-dependencies]
                                nonexistent-style-dependencies))]
      (let [deps-graph (::dependency/deps-graph system)
            result (<! (deps-graph config :app))]
        (is (errors/not-found? result)))))
  (testing "Returns an error if the 'deps' function returns an error"
    (with-system [system
                  (-> sys-config
                      (assoc-in [::dependency/deps :error]
                                (errors/invalid-config "Boom")))]
      (let [deps-graph (::dependency/deps-graph system)
            result (<! (deps-graph config :app))]
        (is (errors/invalid-config? result)))))
  (testing "Returns the dependency graph"
    (with-system [system
                  (-> sys-config
                      (assoc-in [::dependency/deps :fake-dependencies]
                                dependencies))]
      (let [deps-graph (::dependency/deps-graph system)
            result (<! (deps-graph config :app))]
        (are [x y] (ctnd/depends? result x y)
          (src-file "some/namespace.cljs") (src-file "foo/foo.cljs")
          (src-file "some/namespace.cljs") (src-file "foo/baz.cljs")
          (src-file "some/namespace.cljs") (src-file "foo/bar.scss")
          (src-file "some/namespace.cljs") (src-file "foo/bang.scss"))
        (are [x y] (not (ctnd/depends? result x y))
          (src-file "foo/bang.scss") (src-file "some/namespace.cljs"))))))