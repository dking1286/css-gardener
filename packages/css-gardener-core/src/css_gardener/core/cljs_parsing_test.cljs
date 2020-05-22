(ns css-gardener.core.cljs-parsing-test
  (:require [clojure.core.async :refer [go <!]]
            [clojure.string :as string]
            [clojure.test :refer [deftest is use-fixtures]]
            [css-gardener.core.cljs-parsing
             :as cljs
             :refer [deps-from-ns-decl
                     ns-name->relative-path
                     ns-name->possible-absolute-paths
                     ns-name->absolute-path
                     stylesheet-deps-from-ns-decl]]
            [css-gardener.core.logging :as logging]
            [css-gardener.core.system :as system]
            [css-gardener.core.utils.errors :as errors]
            [css-gardener.core.utils.fs :as fs]
            [css-gardener.core.utils.testing :refer [instrument-specs
                                                     deftest-async
                                                     testing
                                                     with-system]]
            [path]))

(use-fixtures :once instrument-specs)

(def ^:private cwd (path/resolve "."))

(deftest t-deps-from-ns-decl
  (testing "parses namespace dependencies"
    (let [ns-decl '(ns hello.world
                     (:require [fs]
                               [clojure.string :as string]))]
      (is (= '#{fs clojure.string}
             (deps-from-ns-decl ns-decl)))))
  (testing "parses namespace dependencies with strings for namespaces"
    (let [ns-decl '(ns hello.world
                     (:require ["fs"]
                               [clojure.string :as string]))]
      (is (= '#{"fs" clojure.string}
             (deps-from-ns-decl ns-decl))))))

(deftest t-ns-name->relative-path
  (testing "Converts symbol namespaces to file names"
    (is (= "hello/world/core"
           (ns-name->relative-path 'hello.world.core))))
  (testing "Converts kebab-case names into snake_case"
    (is (= "hello/world/other_namespace"
           (ns-name->relative-path 'hello.world.other-namespace))))
  (testing "Converts string namespaces into file names"
    (is (= "@css-gardener/sass-transformer"
           (ns-name->relative-path "@css-gardener/sass-transformer")))))

(deftest t-ns-name->possible-absolute-paths
  (testing "Converts symbol namespaces to possible absolute paths"
    (is (= #{(str cwd "/src/hello/world.cljs")
             (str cwd "/src/hello/world.cljc")
             (str cwd "/test/hello/world.cljs")
             (str cwd "/test/hello/world.cljc")}
           (ns-name->possible-absolute-paths 'hello.world ["src" "test"])))))

(deftest-async t-ns-name->absolute-path
  (testing "returns a channel with nil when no file matches the ns-name"
    (let [ns-name 'hello.world
          source-paths ["src" "test"]
          exists? (fn [_] (go false))]
      (is (errors/not-found? (<! (ns-name->absolute-path exists? source-paths ns-name))))))
  (testing "returns the absolute path when one file matches the ns-name"
    (let [ns-name 'hello.world
          file-name (str cwd "/src/hello/world.cljs")
          source-paths ["src" "test"]
          exists? (fn [name] (go (= name file-name)))]
      (is (= file-name
             (<! (ns-name->absolute-path exists? source-paths ns-name))))))
  (testing "returns a conflict error when more than one file matches the ns-name"
    (let [ns-name 'hello.world
          source-paths ["src" "test"]
          exists? (fn [_] (go true))]
      (is (errors/conflict? (<! (ns-name->absolute-path exists? source-paths ns-name)))))))

(deftest t-stylesheet-deps-from-ns-decl
  (testing "returns an empty set when there is no css-gardener/require metadata on the namespace name"
    (let [ns-decl '(ns hello.world)]
      (is (empty? (stylesheet-deps-from-ns-decl ns-decl "/path/to/current/file")))))
  (testing "returns an absolute path when css-gardener/require contains a path relative to the source-paths"
    (let [ns-decl '(ns ^{:css-gardener/require ["./styles.scss"]} hello.world)]
      (is #{"/path/to/current/styles.scss"}
          (stylesheet-deps-from-ns-decl ns-decl "/path/to/current/file")))))

(defn- src-file
  [relative-path]
  (str cwd "/src/" relative-path))

(def ^:private files
  {(src-file "some/other/namespace.cljs") ""})

(def ^:private sys-config
  (-> system/config
      (assoc-in [::fs/exists? :files] files)
      (assoc-in [::logging/logger :level] :debug)
      (assoc-in [::logging/logger :sinks] #{:cache})))

(deftest-async t-cljs-deps
  (testing "returns the set of all dependencies"
    (with-system [system sys-config]
      (let [absolute-path (str cwd "/src/hello/world.cljs")
            content (binding [*print-meta* true]
                      (pr-str '(ns
                                 ^{:css-gardener/require ["./styles.scss"]}
                                 hello.world
                                 (:require [some.other.namespace]))))
            file {:absolute-path absolute-path :content content}
            source-paths ["src" "test"]
            cljs-deps (::cljs/deps system)]
        (is (= #{(str cwd "/src/some/other/namespace.cljs")
                 (str cwd "/src/hello/styles.scss")}
               (<! (cljs-deps file source-paths)))))))
  (testing "logs a warning and skips namespace dependencies where no matching
            file can be found"
    (with-system [system sys-config]
      (let [absolute-path (str cwd "/src/hello/world.cljs")
            content (binding [*print-meta* true]
                      (pr-str '(ns
                                 ^{:css-gardener/require ["./styles.scss"]}
                                 hello.world
                                 (:require [some.other.namespace]
                                           [does.not.exist]))))
            file {:absolute-path absolute-path :content content}
            source-paths ["src" "test"]
            logger (::logging/logger system)
            cljs-deps (::cljs/deps system)]
        (is (= #{(str cwd "/src/some/other/namespace.cljs")
                 (str cwd "/src/hello/styles.scss")}
               (<! (cljs-deps file source-paths))))
        (is (logging/has-message?
             logger
             #(string/includes? (.getMessage %)
                                "No file matching namespace does.not.exist")))))))