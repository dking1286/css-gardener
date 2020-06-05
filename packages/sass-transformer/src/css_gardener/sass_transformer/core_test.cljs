(ns css-gardener.sass-transformer.core-test
  (:require [clojure.test :refer [deftest async testing is]]
            [css-gardener.sass-transformer.core :refer [exit]]
            [goog.object :as object]))

(deftest t-exit-yields-file-unmodified
  (testing "exit yields the file unmodified"
    (async done
      (exit #js {:absolutePath "/hello/world"
                 :content "hello world"}
            #js {}
            (fn [err result]
              (is (nil? err))
              (is (object/equals #js {:absolutePath "/hello/world"
                                      :content "hello world"}
                                 result))
              (done))))))