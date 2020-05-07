(ns css-gardener.core.utils.async-test
  (:require [clojure.core.async :refer [go <! >! chan close!]]
            [clojure.test :refer [testing is]]
            [css-gardener.core.utils.async :refer [take-all await-all flat-map]]
            [css-gardener.core.utils.errors :as errors]
            [css-gardener.core.utils.testing :refer [deftest-async]]))

(deftest-async t-take-all
  (testing "Yields deadline-exceeded if the source channel does not close within the timeout"
    (let [source (chan)] ;; Never closes
      (is (errors/deadline-exceeded? (<! (take-all 50 source))))))
  (testing "Yields a seq of the results if the source channel closes within the timeout"
    (let [source (chan)]
      (go
        (doseq [i (range 5)]
          (>! source i))
        (close! source))
      (is (= [0 1 2 3 4] (<! (take-all 1000 source))))))
  (testing "Does not terminate early on error by default"
    (let [source (chan)]
      (go
        (>! source (errors/conflict "Boom"))
        (doseq [i (range 5)]
          (>! source i))
        (close! source))
      (let [result (<! (take-all 1000 source))]
        (is (= (ex-data (errors/conflict "Boom"))
               (ex-data (first result))))
        (is (= [0 1 2 3 4]
               (rest result))))))
  (testing "Returns the first error encountered if bailout-on-error is true"
    (let [source (chan)]
      (go
        (>! source (errors/conflict "Boom"))
        (doseq [i (range 5)]
          (>! source i))
        (close! source))
      (let [result (<! (take-all 1000 source :bailout-on-error? true))]
        (is (errors/conflict? result))))))

(deftest-async t-await-all
  (testing "Yields deadline-exceeded if any of the source channels do not close within the timeout"
    (let [sources [(go 1) (chan) (go 3)]] ;; Second one never closes
      (is (errors/deadline-exceeded? (<! (await-all 50 sources))))))
  (testing "Yields an error if any of the source channels yield an error"
    (let [sources [(go 1) (go (errors/conflict "Boom")) (go 3)]]
      (is (errors/conflict? (<! (await-all 1000 sources))))))
  (testing "Yields a sequence of results if all of the source channels yield non-error results within the timeout"
    (let [sources [(go 1) (go 2) (go 3)]]
      (is (= [1 2 3] (sort (<! (await-all 1000 sources))))))))

(deftest-async t-flat-map
  (testing "Yields the error if the function throws an error"
    (let [f #(throw (js/Error. "Boom"))
          ch (go 1)]
      (is (= "Boom"
             (.-message (<! (flat-map f ch)))))))
  (testing "Propagates the error if the input channel contains an error"
    (let [f #(go 3)
          ch (go (js/Error. "Boom"))]
      (is (= "Boom"
             (.-message (<! (flat-map f ch)))))))
  (testing "Yields the value of the inner channel"
    (let [f #(go 2)
          ch (go 1)]
      (is (= 2 (<! (flat-map f ch)))))))