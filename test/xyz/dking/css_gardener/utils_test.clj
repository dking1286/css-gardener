(ns xyz.dking.css-gardener.utils-test
  (:require [clojure.test :refer :all]
            [me.raynes.fs :as fs]
            [xyz.dking.css-gardener.utils :as utils]))

(deftest map-vals-test
  (testing "returns a transducer when called with arity 1"
    (is (= {:hello 2 :world 3}
           (into {} (utils/map-vals inc) {:hello 1 :world 2}))))
  (testing "maps over a seq when called with arity 2"
    (is (= {:hello 2 :world 3}
           (into {} (utils/map-vals inc {:hello 1 :world 2}))))))

(deftest unique-by-test
  (testing "returns nil when nil is passed in."
    (is (= nil
           (utils/unique-by identity nil))))
  (testing "returns an empty sequence if the passed in sequence is empty"
    (is (= '()
           (utils/unique-by identity []))))
  (testing "returns the original sequence unmodified if the items are already unique"
    (is (= '("hello" "world")
           (utils/unique-by identity ["hello" "world"]))))
  (testing "returns a sequence of unique values"
    (is (= '("hello" "world")
           (utils/unique-by identity ["hello" "hello" "world"]))))
  (testing "uses the passed-in function to determine uniqueness"
    (is (= '({:name "hello"} {:name "world"})
           (utils/unique-by :name [{:name "hello"}
                                   {:name "hello"}
                                   {:name "world"}])))))

(deftest unique-files-test
  (testing "returns a list of unique files"
    (is (= [(fs/absolute "test/xyz/dking/css_gardener/test_example/no_style_vars.cljs")
            (fs/absolute "test/xyz/dking/css_gardener/test_example/style_vars.cljs")]
           (utils/unique-files ["test/xyz/dking/css_gardener/test_example/style_vars.cljs"
                                "test/xyz/dking/css_gardener/test_example/style_vars.cljs"
                                "test/xyz/dking/css_gardener/test_example/no_style_vars.cljs"]))))
  (testing "excludes non-existent files"
    (is (= [(fs/absolute "test/xyz/dking/css_gardener/test_example/no_style_vars.cljs")
            (fs/absolute "test/xyz/dking/css_gardener/test_example/style_vars.cljs")]
           (utils/unique-files ["test/xyz/dking/css_gardener/test_example/style_vars.cljs"
                                "test/xyz/dking/css_gardener/test_example/blah.cljs"
                                "test/xyz/dking/css_gardener/test_example/no_style_vars.cljs"]))))
  (testing "expands globs"
    (is (= [(fs/absolute "test/xyz/dking/css_gardener/test_example/computed_style_var.cljs")
            (fs/absolute "test/xyz/dking/css_gardener/test_example/multiple_style_vars.cljs")
            (fs/absolute "test/xyz/dking/css_gardener/test_example/no_style_vars.cljs")
            (fs/absolute "test/xyz/dking/css_gardener/test_example/style_vars.cljs")]
            (utils/unique-files ["test/xyz/dking/css_gardener/test_example/style_vars.cljs"
                                 "test/xyz/dking/css_gardener/test_example/*.cljs"])))))
