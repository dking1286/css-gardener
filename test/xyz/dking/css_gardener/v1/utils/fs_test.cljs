(ns xyz.dking.css-gardener.v1.utils.fs-test
  (:require [clojure.core.async :refer [go <!]]
            [clojure.test :refer [deftest testing is async]]
            [xyz.dking.css-gardener.v1.utils.fs :as sut]
            [xyz.dking.css-gardener.v1.utils.errors :as errors]
            [clojure.string :as string]))

(deftest glob-not-found
  (testing "returns a channel that yields an empty seq when no files match the glob"
    (async done
      (go
        (is (= [] (<! (sut/glob "/blah/*.cljs"))))
        (done)))))

(deftest glob-one-star
  (testing "returns a channel that yields a set of file paths matching the pattern when there is one star in the pattern"
    (async done
      (go
        (is (= ["test/xyz/dking/css_gardener/test_example/nested/no_style_vars.cljs"]
               (<! (sut/glob "test/xyz/dking/css_gardener/test_example/nested/*.cljs"))))
        (done)))))

(deftest glob-double-star
  (testing "returns a channel that yields a set of file paths matching the pattern when there is a recursive double star in the pattern"
    (async done
      (go
        (is (= ["test/xyz/dking/css_gardener/test_example/nested/nested/no_style_vars.cljs"
                "test/xyz/dking/css_gardener/test_example/nested/no_style_vars.cljs"]
               (<! (sut/glob "test/xyz/dking/css_gardener/test_example/nested/**/*.cljs"))))
        (done)))))

(deftest unique-files-expands-globs
  (testing "expands globs passed in"
    (async done
      (go
        (is (= ["test/xyz/dking/css_gardener/test_example/nested/nested/no_style_vars.cljs"
                "test/xyz/dking/css_gardener/test_example/nested/no_style_vars.cljs"]
               (<! (sut/unique-files ["test/xyz/dking/css_gardener/test_example/nested/**/*.cljs"]))))
        (done)))))

(deftest unique-files-removes-duplicates
  (testing "expands globs, removing duplicate files"
    (async done
      (go
        (is (= ["test/xyz/dking/css_gardener/test_example/nested/nested/no_style_vars.cljs"
                "test/xyz/dking/css_gardener/test_example/nested/no_style_vars.cljs"]
               (<! (sut/unique-files ["test/xyz/dking/css_gardener/test_example/nested/**/*.cljs"
                                      "test/xyz/dking/css_gardener/test_example/nested/**/*.cljs"]))))
        (done)))))

(deftest exists-no-file-test
  (testing "returns a channel that yields false when the file does not exist"
    (async done
      (go
        (is (= false (<! (sut/exists? "./blah/blah"))))
        (done)))))

(deftest exists-yes-file-test
  (testing "returns a channel that yields true when the file exists"
    (async done
      (go
        (is (= true (<! (sut/exists? "./package.json"))))
        (done)))))

(deftest read-file-does-not-exist
  (testing "returns a channel that yields a not-found error when the file does not exist"
    (async done
      (go
        (is (errors/not-found? (<! (sut/read-file "./blah/blah"))))
        (done)))))

(deftest read-file-throws-other-error
  (testing "returns a channel that yields an error when an unknown error happens while reading the file"))

(deftest read-file-exists
  (testing "returns a channel that yields the file contents as a string when reading is successful"
    (async done
      (go
        (is (string/includes? (<! (sut/read-file "./package.json"))
                              "\"name\": \"css-gardener\""))
        (done)))))
