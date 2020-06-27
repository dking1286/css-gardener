(ns css-gardener.core.caching-test
  (:require [clojure.core.async :refer [go <!]]
            [clojure.test :refer [is]]
            [css-gardener.core.caching :as caching]
            [css-gardener.core.utils.testing :refer [deftest-async testing]]))

(deftest-async t-with-cache
  (testing "Sets the value in the cache if it doesn't already exist"
    (let [cache (caching/new-in-memory-cache {})]
      (is (= 3
             (<! (caching/with-cache cache "the-key"
                   (go 3)))))
      (is (= 3
             (<! (caching/get cache "the-key"))))))
  (testing "Returns the value from the cache and does not evaluate the body
            if it already exists."
    (let [cache (caching/new-in-memory-cache {"the-key" 3})
          num-calls (atom 0)]
      (is (= 3
             (<! (caching/with-cache cache "the-key"
                   (swap! num-calls inc)
                   (go 3)))))
      (is (= 0 @num-calls)))))