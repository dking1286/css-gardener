(ns xyz.dking.css-gardener.analyzer-test
  (:require [clojure.test :refer :all]
            [clojure.string :as string]
            [xyz.dking.css-gardener.analyzer :as analyzer]))

(deftest all-style-vars-test
  (testing "returns all style vars in the passed-in files"
    (let [files ["test/xyz/dking/css_gardener/test_example/style_vars.cljs"
                 "test/xyz/dking/css_gardener/test_example/multiple_style_vars.cljs"]]
      (is (= '(xyz.dking.css-gardener.test-example.style-vars/style
               xyz.dking.css-gardener.test-example.multiple-style-vars/style1
               xyz.dking.css-gardener.test-example.multiple-style-vars/style2)
             (analyzer/all-style-vars files)))))
  (testing "returns an empty seq when none of the passed files contain style vars"
    (let [files ["test/xyz/dking/css_gardener/test_example/no_style_vars.cljs"]]
      (is (= '()
             (analyzer/all-style-vars files)))))
  (testing "logs a warning when a passed-in file does not contain any style vars"
    (let [files ["test/xyz/dking/css_gardener/test_example/no_style_vars.cljs"]
          logged (with-out-str (analyzer/all-style-vars files))]
      (is (string/includes? logged
                            (str "File test/xyz/dking/css_gardener/"
                                 "test_example/no_style_vars.cljs "
                                 "does not contain any style definitions.")))))
  (testing "logs a warning when a passed-in file is not a cljs file"
    (let [files ["test/xyz/dking/css_gardener/test_example/not_a_cljs_file.scss"]
          logged (with-out-str (analyzer/all-style-vars files))]
      (is (string/includes? logged
                            (str "File test/xyz/dking/css_gardener/"
                                 "test_example/not_a_cljs_file.scss "
                                 "is not a CLJS file, skipping.")))))
  (testing "logs a warning when a passed-in file does not exist"
    (let [files ["test/xyz/dking/css_gardener/test_example/does_not_exist.cljs"]
          logged (with-out-str (analyzer/all-style-vars files))]
      (is (string/includes? logged
                            (str "File test/xyz/dking/css_gardener/"
                                 "test_example/does_not_exist.cljs "
                                 "does not exist, skipping."))))))


