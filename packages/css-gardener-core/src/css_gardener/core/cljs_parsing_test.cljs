(ns css-gardener.core.cljs-parsing-test
  (:require ["path" :as path]
            [clojure.core.async :refer [chan go <! >!]]
            [clojure.test :refer [deftest testing is async]]
            [css-gardener.core.cljs-parsing :refer [deps-from-ns-decl
                                                    ns-name->relative-path
                                                    ns-name->possible-absolute-paths
                                                    ns-name->absolute-path]]
            [css-gardener.core.utils.errors :as errors]))

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

(def cwd (path/resolve "."))

(deftest t-ns-name->possible-absolute-paths
  (testing "Converts symbol namespaces to possible absolute paths"
    (is (= #{(str cwd "/src/hello/world.cljs")
             (str cwd "/src/hello/world.cljc")
             (str cwd "/test/hello/world.cljs")
             (str cwd "/test/hello/world.cljc")}
           (ns-name->possible-absolute-paths 'hello.world ["src" "test"])))))

(deftest t-ns-name->absolute-path
  (async done
    (go
      (testing "returns a channel with nil when no file matches the ns-name"
        (let [ns-name 'hello.world
              source-paths ["src" "test"]
              exists? (fn [_] (go false))]
          (is (nil? (<! (ns-name->absolute-path ns-name source-paths exists?))))))
      (testing "returns the absolute path when one file matches the ns-name"
        (let [ns-name 'hello.world
              file-name (str cwd "/src/hello/world.cljs")
              source-paths ["src" "test"]
              exists? (fn [name] (go (= name file-name)))]
          (is (= file-name
                 (<! (ns-name->absolute-path ns-name source-paths exists?))))))
      (testing "returns a conflict error when more than one file matches the ns-name"
        (let [ns-name 'hello.world
              source-paths ["src" "test"]
              exists? (fn [_] (go true))]
          (is (errors/conflict? (<! (ns-name->absolute-path ns-name source-paths exists?))))))
      (done))))