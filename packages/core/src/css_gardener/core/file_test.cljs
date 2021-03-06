(ns css-gardener.core.file-test
  (:require [clojure.core.async :refer [go <!]]
            [clojure.string :as s]
            [clojure.test :refer [deftest is use-fixtures async]]
            [css-gardener.core.file :as sut]
            [css-gardener.core.utils.errors :as errors]
            [css-gardener.core.utils.fs :as fs]
            [css-gardener.core.utils.testing :refer [testing
                                                     instrument-specs]]
            [path]))

(use-fixtures :once instrument-specs)

(def ^:private cwd (path/resolve "."))

(deftest t-from-path
  (async done
    (go
      (testing "returns an error when the file does not exist"
        (is (errors/not-found? (<! (sut/from-path fs/read-file "blah.js")))))
      (testing "returns the file map when the file does exist"
        (let [result (<! (sut/from-path fs/read-file "index.js"))]
          (is (= (str cwd "/index.js")
                 (:absolute-path result)))
          (is (s/includes? (:content result) "require('./dist/main')"))))
      (done))))