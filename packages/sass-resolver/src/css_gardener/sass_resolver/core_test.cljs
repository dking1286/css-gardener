(ns css-gardener.sass-resolver.core-test
  (:require [clojure.test :refer [deftest testing is async]]
            [css-gardener.sass-resolver.core :refer [main]]
            [goog.array :as array]))

(deftest t-main-no-deps
  (testing "Yields an empty array if the sass file has no dependencies"
    (async done
      (main #js {:absolutePath "/some/style/file.scss"
                 :content ""}
            #js {}
            (fn [error result]
              (is (nil? error))
              (is (array/isEmpty result))
              (done))))))
