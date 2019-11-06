(ns xyz.dking.css-gardener.utils-test
  (:require [clojure.test :refer :all]
            [xyz.dking.css-gardener.utils :as utils]))

(deftest map-vals-test
  (testing "returns a transducer when called with arity 1"
    (is (= {:hello 2 :world 3}
           (into {} (utils/map-vals inc) {:hello 1 :world 2}))))
  (testing "maps over a seq when called with arity 2"
    (is (= {:hello 2 :world 3}
           (into {} (utils/map-vals inc {:hello 1 :world 2}))))))
