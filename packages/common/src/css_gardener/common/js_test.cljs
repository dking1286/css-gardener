(ns css-gardener.common.js-test
  (:require [clojure.test :refer [deftest testing is]]
            [css-gardener.common.js :refer [from-js to-js]]
            [goog.object :as gobj]))

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