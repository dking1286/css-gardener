(ns css-gardener.core.utils.js-test
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [css-gardener.core.utils.js :refer [from-js to-js]]
            [css-gardener.core.utils.testing :refer [testing
                                                     instrument-specs]]
            [goog.object :as gobj]))

(use-fixtures :each instrument-specs)

(deftest t-from-js
  (testing "returns a clj data structure, with camelCase keys transformed
            to selector-case"
    (is (= {:some-key "someValue"}
           (from-js #js {"someKey" "someValue"})))))

(deftest t-to-js
  (testing "returns a js data structure, with selector-case keys transformed
            to camelCase"
    (is (= "someValue"
           (-> {:some-key "someValue"}
               to-js
               (gobj/get "someKey"))))))