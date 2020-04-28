(ns css-gardener.core.file-test
  (:require ["path" :as path]
            [clojure.core.async :refer [go <!]]
            [clojure.string :as s]
            [clojure.test :refer [deftest testing is use-fixtures async]]
            [css-gardener.core.file :as sut]
            [css-gardener.core.utils.errors :as errors]
            [css-gardener.core.utils.testing :refer [instrument-specs]]))

(use-fixtures :once instrument-specs)

(def cwd (path/resolve "."))

(deftest t-from-path
  (async done
    (go
      (testing "returns an error when the file does not exist"
        (is (errors/not-found? (<! (sut/from-path "blah.js")))))
      (testing "returns the file map when the file does exist"
        (let [result (<! (sut/from-path "index.js"))]
          (is (= (str cwd "/index.js")
                 (:absolute-path result)))
          (is (s/includes? (:content result) "require('./dist/main')"))))
      (done))))