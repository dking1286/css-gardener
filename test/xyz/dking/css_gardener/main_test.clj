(ns xyz.dking.css-gardener.main-test
  (:require [clojure.string :as string]
            [clojure.test :refer :all]
            [xyz.dking.css-gardener.main :refer [main help-message]]
            [xyz.dking.css-gardener.test-helpers :refer [*temp-file*
                                                         with-temp-file]]))

(use-fixtures :each with-temp-file)

(deftest main-test
  (testing "prints a help message when --help option is provided"
    (is (string/includes? (with-out-str @(main "--help"))
                          help-message)))
  (testing "prints a message when the default configuration file is not found"
    (is (string/includes? (with-out-str @(main "build"
                                               "--type" "scss"
                                               "--input-files" "test/xyz/dking/css_gardener/test_example/style_vars.cljs,test/xyz/dking/css_gardener/test_example/multiple_style_vars.cljs"
                                               "--output-file" (.getAbsolutePath *temp-file*)))
                          "not found")))
  (testing "prints a warning message when the custom configuration file is not found"
    (is (string/includes? (with-out-str @(main "build"
                                               "--type" "scss"
                                               "--config-file" "i-do-not-exist"
                                               "--input-files" "test/xyz/dking/css_gardener/test_example/style_vars.cljs,test/xyz/dking/css_gardener/test_example/multiple_style_vars.cljs"
                                               "--output-file" (.getAbsolutePath *temp-file*)))
                          "WARNING")))
  (testing "builds a stylesheet from the passed-in options"
    @(main "build"
           "--type" "scss"
           "--input-files" "test/xyz/dking/css_gardener/test_example/scss/_styles1.scss,test/xyz/dking/css_gardener/test_example/scss/_styles2.scss"
           "--output-file" (.getAbsolutePath *temp-file*))
    (let [styles (slurp *temp-file*)]
      (is (string/includes? styles "background-color: green"))
      (is (string/includes? styles "color: red")))))


