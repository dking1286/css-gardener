(ns css-gardener.core.cljs-parsing-test
  (:require [clojure.test :refer [deftest testing is]]
            [css-gardener.core.cljs-parsing :refer [deps-from-ns-decl]]))

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