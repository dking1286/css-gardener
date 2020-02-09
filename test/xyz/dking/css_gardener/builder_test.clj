(ns xyz.dking.css-gardener.builder-test
  (:require [xyz.dking.css-gardener.builder :as sut]
            [clojure.test :refer :all]
            [clojure.tools.namespace.dependency :as dependency]))

(deftest get-dependency-graph-test
  (testing "returns a dependency graph of the passed in files"
    (let [files [{:file "/some/file"
                  :text "Some text"}
                 {:file "/some/other/file"
                  :text "Some other text"}
                 {:file "/some/third/file"
                  :text "Some third text"}]
          builder-config {:output-prefix "Input was"
                          :dependencies {"/some/file" ["/some/dependency"]
                                         "/some/other/file" ["/some/dependency"
                                                             "/some/other/dependency"]}
                          :error? false}
          builder (sut/new-stub-builder builder-config)
          graph (sut/get-dependency-graph builder files)]
      (are [x y] (dependency/depends? graph x y)
        "/some/file" "/some/dependency"
        "/some/other/file" "/some/dependency"
        "/some/other/file" "/some/other/dependency")
      (are [x y] (not (dependency/depends? graph x y))
        "/some/third/file" "/some/dependency"
        "/some/third/file" "/some/other/dependency"
        "/some/file" "/some/other/file"))))

