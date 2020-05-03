(ns css-gardener.core.dependency-test
  (:require ["path" :as path]
            [clojure.core.async :refer [<!]]
            [clojure.test :refer [testing is use-fixtures]]
            [css-gardener.core.dependency :as dependency]
            [css-gardener.core.logging :as logging]
            [css-gardener.core.modules :as modules]
            [css-gardener.core.system :as system]
            [css-gardener.core.utils.errors :as errors]
            [css-gardener.core.utils.fs :as fs]
            [css-gardener.core.utils.testing :refer [instrument-specs
                                                     with-system
                                                     deftest-async]]))

(use-fixtures :once instrument-specs)

(def cwd (path/resolve "."))

(def config
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

(def sys-config
  (-> system/config
      (assoc-in [::fs/exists? :existing-files]
                #{(str cwd "/src/hello/world.cljs")
                  (str cwd "/src/some/other/namespace.cljs")
                  (str cwd "/src/some/third/namespace.cljs")})
      (assoc-in [::modules/load :return-value] nil)
      (assoc-in [::logging/logger :level] :debug)
      (assoc-in [::logging/logger :sinks] #{:cache})))

(def ns-decl
  '(ns hello.world
     (:require [some.other.namespace]
               [some.third.namespace])))

(deftest-async t-deps
  (testing "returns the cljs deps if the file is a cljs file"
    (with-system [system sys-config]
      (let [deps (::dependency/deps system)
            file {:absolute-path (str cwd "/src/hello/world.cljs")
                  :content (pr-str ns-decl)}]
        (is (= #{(str cwd "/src/some/other/namespace.cljs")
                 (str cwd "/src/some/third/namespace.cljs")}
               (<! (deps file config)))))))
  (testing "returns invalid-config if no rule matches the file"
    (with-system [system sys-config]
      (let [deps (::dependency/deps system)
            file {:absolute-path (str cwd "/src/hello/world.blah")
                  :content "blah"}]
        (is (errors/invalid-config? (<! (deps file config)))))))
  (testing "returns invalid-config if multiple rules match the file"
    (with-system [system sys-config]
      (let [deps (::dependency/deps system)
            file {:absolute-path (str cwd "/src/hello/world.blah")
                  :content "blah"}
            config (-> config
                       (assoc-in [:rules #"blah$"] {:transformers []})
                       (assoc-in [:rules #"lah$"] {:transformers []}))]
        (is (errors/invalid-config? (<! (deps file config)))))))
  (testing "returns an empty set if the matching rule has no dependency resolver"
    (with-system [system sys-config]
      (let [deps (::dependency/deps system)
            file {:absolute-path (str cwd "/src/hello/world.blah")
                  :content "blah"}
            config (-> config
                       (assoc-in [:rules #"blah$"] {:transformers []}))]
        (is (= #{} (<! (deps file config)))))))
  (testing "returns the value returned by the dependency resolver if one exists"
    (let [sys-config
          (-> sys-config
              (assoc-in [::modules/load :return-value]
                        (fn [_ cb] (cb nil #{"/some/other/namespace.cljs"}))))

          file
          {:absolute-path (str cwd "/src/hello/world.blah")
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
          {:absolute-path (str cwd "/src/hello/world.blah")
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

;; (deftest t-get-entries
;;   (testing "Returns a set of all of the entry namespaces from the config"
;;     (is (= '#{some.namespace some.other.namespace some.third.namespace}
;;            (dependency/get-entries config :app)))))

;; (deftest-async t-deps-graph
;;   (testing "Logs a message at the info level"
;;     (with-system [system sys-config]
;;       (let [logger (::logging/logger system)
;;             deps-graph (::dependency/deps-graph system)]
;;         (deps-graph {} :app)
;;         (is (seq (->> @(:cache logger)
;;                       (filter #(= "Building dependency graph..."
;;                                   (.getMessage %)))))))))
;;   (testing "Returns an error if there is a circular dependency")
;;   (testing "Returns an error if a cljs file refers to a dependency that does not exist")
;;   (testing "Returns an error if a style file refers to a dependency that does not exist")
;;   (testing "Returns the dependency graph"))