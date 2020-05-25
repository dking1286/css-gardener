(ns css-gardener.core.transformation-test
  (:require [clojure.core.async :refer [<!]]
            [clojure.spec.alpha :as s]
            [clojure.string :as string]
            [clojure.test :refer [deftest is use-fixtures run-tests]]
            [css-gardener.core.config :as config]
            [css-gardener.core.dependency :as dependency]
            [css-gardener.core.logging :as logging]
            [css-gardener.core.modules :as modules]
            [css-gardener.core.system :as system]
            [css-gardener.core.transformation :as transformation]
            [css-gardener.core.utils.errors :as errors]
            [css-gardener.core.utils.testing :refer [deftest-async
                                                     testing
                                                     with-system
                                                     instrument-specs]]
            [goog.object :as gobj]
            [integrant.core :as ig]))

(use-fixtures :each instrument-specs)

(def ^:private modules
  {{:node-module "@css-gardener/scope-transformer"}
   (transformation/transformer-stub
    nil {:absolute-path "" :content ""})

   {:node-module "@css-gardener/sass-transformer"}
   (transformation/transformer-stub
    nil {:absolute-path "" :content ""})

   {:node-module "@css-gardener/sass-resolver"}
   (dependency/resolver-stub nil #js [])})

(def ^:private config
  {:source-paths ["src"]

   :builds
   {:app {:target :browser
          :output-dir "public/js"
          :asset-path "/js"
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

(def ^:private sys-config
  (-> system/config
      (assoc ::config/config config)
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
        (is (= 2 (count transformers)))
        (is (s/valid? ::transformation/transformer-config
                      (get transformers
                           {:node-module "@css-gardener/sass-transformer"})))))))

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

(comment
  (run-tests))
