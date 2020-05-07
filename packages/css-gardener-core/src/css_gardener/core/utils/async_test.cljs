(ns css-gardener.core.utils.async-test
  (:refer-clojure :exclude [map])
  (:require [clojure.core.async :refer [go <! >! chan close!]]
            [clojure.test :refer [testing is]]
            [css-gardener.core.utils.async :refer [callback->channel
                                                   node-callback->channel
                                                   map
                                                   flat-map
                                                   take-all
                                                   await-all]]
            [css-gardener.core.utils.errors :as errors]
            [css-gardener.core.utils.testing :refer [deftest-async]]))

(deftest-async t-callback->channel
  (testing "Yields the value returned by the callback"
    (is (= 2
           (<! (callback->channel (fn [num cb] (cb num))
                                  1
                                  (fn [num] (inc num)))))))
  (testing "Yields an error if the callback function throws an error"
    (is (= "Boom"
           (.-message (<! (callback->channel (fn [num cb] (cb num))
                                             1
                                             (fn [_] (throw (js/Error. "Boom")))))))))
  (testing "Yields an error if the function throws an error"
    (is (= "Boom"
           (.-message (<! (callback->channel (fn [_ _] (throw (js/Error. "Boom")))
                                             1
                                             (fn [num] (inc num)))))))))

(deftest-async t-node-callback->channel
  (testing "Yields an error if the function throws an error"
    (is (= "Boom"
           (.-message (<! (node-callback->channel
                           (fn [_ _] (throw (js/Error. "Boom")))
                           1
                           (fn [err val] (or err val))))))))
  (testing "Yields an error if the callback function throws an error"
    (is (= "Boom"
           (.-message (<! (node-callback->channel
                           (fn [num cb] (cb nil num))
                           1
                           (fn [_ _] (throw (js/Error. "Boom")))))))))
  (testing "Yields the value returned by the callback"
    (is (= 2
           (<! (node-callback->channel
                (fn [num cb] (cb nil num))
                1
                (fn [_ val] (inc val))))))))

(deftest-async t-map
  (testing "Propagates the error unmodified if the input channel yields an error"
    (let [f inc
          ch (go (ex-info "Boom" {}))]
      (is (= "Boom"
             (.-message (<! (map f ch)))))))
  (testing "Yields the error if the function throws an error"
    (let [f #(throw (ex-info "Boom" {}))
          ch (go 3)]
      (is (= "Boom"
             (.-message (<! (map f ch)))))))
  (testing "Yields the value returned by the passed-in function"
    (let [f inc
          ch (go 3)]
      (is (= 4
             (<! (map f ch)))))))

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
    (let [f #(go (inc %))
          ch (go 1)]
      (is (= 2 (<! (flat-map f ch)))))))

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
