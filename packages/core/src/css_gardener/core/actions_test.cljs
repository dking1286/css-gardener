(ns css-gardener.core.actions-test
  (:require [clojure.core.async :refer [<!]]
            [clojure.test :refer [is]]
            [css-gardener.core.actions :as actions]
            [css-gardener.core.caching :as caching]
            [css-gardener.core.utils.testing :refer [deftest-async
                                                     testing]]))

(deftest-async t-apply
  (testing "::remove-from-cache"
    (testing "Does nothing if the path does not exist in the cache"
      (let [initial {"/foo/baz.cljs" {}}
            cache (caching/new-in-memory-cache initial)]
        (<! (actions/apply (actions/->RemoveFromCache "/foo/bar.cljs")
                           {::caching/compilation-cache cache}))
        (is (= initial @(:cache-atom cache)))))
    (testing "Removes the path from the cache if it exists"
      (let [initial {"/foo/baz.cljs" {}}
            cache (caching/new-in-memory-cache initial)]
        (<! (actions/apply (actions/->RemoveFromCache "/foo/baz.cljs")
                           {::caching/compilation-cache cache}))
        (is (= {} @(:cache-atom cache)))))))