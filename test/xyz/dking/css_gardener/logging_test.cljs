(ns xyz.dking.css-gardener.logging-test
  (:require [clojure.test :refer [deftest testing is are]]
            [xyz.dking.css-gardener.logging :as logging]))

(deftest greater-or-equal-test
  (testing "ordering of log levels"
    (are [x y]
      (logging/greater-or-equal x y)
      :info :debug
      :warn :debug
      :error :debug
      :warn :info)))

(deftest debug-test
  (testing "logs when level is debug"
    (let [l (logging/new-logger {:level :debug})]
      (is (= "hello\n"
             (with-out-str (logging/debug l "hello"))))))
  (testing "does not log when level is info"
    (let [l (logging/new-logger {:level :info})]
      (is (= ""
             (with-out-str (logging/debug l "hello")))))))
