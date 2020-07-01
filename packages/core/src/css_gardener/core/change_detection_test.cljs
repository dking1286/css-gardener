(ns css-gardener.core.change-detection-test
  (:require [clojure.test :refer [deftest testing is]]
            [clojure.tools.namespace.dependency :as ctnd]
            [css-gardener.core.actions :as actions]
            [css-gardener.core.change-detection :as changes]))

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
                     :options {:use-indented-syntax true}}]}}})

(deftest t-get-actions
  (testing "Returns no actions when the changed file is not in the dependency
            graph"
    (let [dependency-graph (-> (ctnd/graph)
                               (ctnd/depend "/foo/foo.cljs" "/foo/bar.scss"))
          absolute-path "/foo/bang.scss"
          new-deps #{}]
      (is (= []
             (changes/get-actions config
                                  dependency-graph
                                  absolute-path
                                  new-deps)))))
  (testing "Returns no actions when the deps have not changed and the changed
            file is not a style file"
    (let [dependency-graph (-> (ctnd/graph)
                               (ctnd/depend "/foo/foo.cljs" "/foo/bar.scss"))
          absolute-path "/foo/foo.cljs"
          new-deps #{"/foo/bar.scss"}]
      (is (= []
             (changes/get-actions config
                                  dependency-graph
                                  absolute-path
                                  new-deps)))))
  (testing "Returns :update-dependency-graph and :recompile when the deps have
            changed and the changed file is not a style file"
    (let [dependency-graph (-> (ctnd/graph)
                               (ctnd/depend "/foo/foo.cljs" "/foo/bar.scss"))
          absolute-path "/foo/foo.cljs"
          new-deps #{"/foo/bar.scss"
                     "/foo/bang.scss"}]
      (is (= [(actions/->UpdateDependencyGraph absolute-path)
              (actions/->Recompile)]
             (changes/get-actions config
                                  dependency-graph
                                  absolute-path
                                  new-deps)))))
  (testing "Returns :remove-from-cache and :recompile for the current file when
            the deps have not changed and the current file is a root style"
    (let [dependency-graph (-> (ctnd/graph)
                               (ctnd/depend "/foo/foo.cljs" "/foo/bar.scss")
                               (ctnd/depend "/foo/bar.scss" "/foo/bang.scss"))
          absolute-path "/foo/bar.scss"
          new-deps #{"/foo/bang.scss"}]
      (is (= [(actions/->RemoveFromCache absolute-path)
              (actions/->Recompile)]
             (changes/get-actions config
                                  dependency-graph
                                  absolute-path
                                  new-deps)))))
  (testing "Returns a :remove-from-cache action for the root styles 
            and :recompile when the deps have not changed and the current file
            is a non-root style"
    (let [dependency-graph (-> (ctnd/graph)
                               (ctnd/depend "/foo/foo.cljs" "/foo/bar.scss")
                               (ctnd/depend "/foo/foo.cljs" "/foo/baz.scss")
                               (ctnd/depend "/foo/bar.scss" "/foo/bang.scss")
                               (ctnd/depend "/foo/baz.scss" "/foo/bang.scss"))
          absolute-path "/foo/bang.scss"
          new-deps #{}]
      (is (= [(actions/->RemoveFromCache "/foo/bar.scss")
              (actions/->RemoveFromCache "/foo/baz.scss")
              (actions/->Recompile)]
             (changes/get-actions config
                                  dependency-graph
                                  absolute-path
                                  new-deps)))))
  (testing "Returns :update-dependency-graph, :remove-from-cache, and :recompile
            when the deps have changed and the changed file is a root style"
    (let [dependency-graph (-> (ctnd/graph)
                               (ctnd/depend "/foo/foo.cljs" "/foo/bar.scss")
                               (ctnd/depend "/foo/bar.scss" "/foo/bang.scss"))
          absolute-path "/foo/bar.scss"
          new-deps #{"/foo/bang.scss" "/foo/baz.scss"}]
      (is (= [(actions/->UpdateDependencyGraph "/foo/bar.scss")
              (actions/->RemoveFromCache "/foo/bar.scss")
              (actions/->Recompile)]
             (changes/get-actions config
                                  dependency-graph
                                  absolute-path
                                  new-deps)))))
  (testing "Returns :update-dependency-graph, :remove-from-cache, and :recompile
            when the deps have changed and the changed file is a non-root style"
    (let [dependency-graph (-> (ctnd/graph)
                               (ctnd/depend "/foo/foo.cljs" "/foo/bar.scss")
                               (ctnd/depend "/foo/foo.cljs" "/foo/baz.scss")
                               (ctnd/depend "/foo/bar.scss" "/foo/bang.scss")
                               (ctnd/depend "/foo/baz.scss" "/foo/bang.scss"))
          absolute-path "/foo/bang.scss"
          new-deps #{"/foo/bears.scss"}]
      (is (= [(actions/->UpdateDependencyGraph "/foo/bang.scss")
              (actions/->RemoveFromCache "/foo/bar.scss")
              (actions/->RemoveFromCache "/foo/baz.scss")
              (actions/->Recompile)]
             (changes/get-actions config
                                  dependency-graph
                                  absolute-path
                                  new-deps))))))