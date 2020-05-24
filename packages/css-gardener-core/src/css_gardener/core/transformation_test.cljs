(ns css-gardener.core.transformation-test
  (:require [clojure.spec.alpha :as s]
            [clojure.string :as string]
            [clojure.test :refer [deftest is use-fixtures]]
            [css-gardener.core.config :as config]
            [css-gardener.core.dependency :as dependency]
            [css-gardener.core.logging :as logging]
            [css-gardener.core.modules :as modules]
            [css-gardener.core.system :as system]
            [css-gardener.core.transformation :as transformation]
            [css-gardener.core.utils.errors :as errors]
            [css-gardener.core.utils.testing :refer [testing
                                                     with-system
                                                     instrument-specs]]
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