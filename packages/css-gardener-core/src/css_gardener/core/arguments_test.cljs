(ns css-gardener.core.arguments-test
  (:require [clojure.string :as string]
            [clojure.test :refer [deftest testing is use-fixtures]]
            [css-gardener.core.arguments :as arguments]
            [css-gardener.core.utils.testing :refer [instrument-specs]]))

(use-fixtures :once instrument-specs)

(deftest t-parse
  (testing "arguments"
    (testing "Converts arguments into keywords"
      (is (= [:watch :app]
             (-> ["watch" "app"]
                 arguments/parse
                 :arguments))))
    (testing "Adds an error to the errors vector if no command is given"
      (is (string/includes? (-> []
                                arguments/parse
                                (get-in [:errors 0]))
                            "No command given")))
    (testing "Adds an error to the errors vector if the command is invalid"
      (is (string/includes? (-> ["blah" "app"]
                                arguments/parse
                                (get-in [:errors 0]))
                            "Invalid command")))
    (testing "Adds an error to the errors vector if the build id is missing"
      (is (string/includes? (-> ["watch"]
                                arguments/parse
                                (get-in [:errors 0]))
                            "Missing build id"))))
  (testing "--help"
    (testing "Detects the --help option"
      (is (true? (get-in (arguments/parse ["--help"]) [:options :help]))))
    (testing "Detects the -h short form for help"
      (is (true? (get-in (arguments/parse ["-h"]) [:options :help])))))
  (testing "--log-level"
    (testing "Detects the --log-level option"
      (is (= :debug
             (get-in (arguments/parse ["watch" "app" "--log-level" "debug"])
                     [:options :log-level]))))
    (testing "Detects the -l short for for log-level"
      (is (= :debug
             (get-in (arguments/parse ["watch" "app" "-l" "debug"])
                     [:options :log-level]))))
    (testing "Defaults log-level to info"
      (is (= :info
             (get-in (arguments/parse ["--help"])
                     [:options :log-level]))))
    (testing "Returns an error if log-level is invalid"
      (is (string/includes? (get-in (arguments/parse ["watch" "app"
                                                      "--log-level" "blah"])
                                    [:errors 0])
                            "Log level must be one of"))))
  (testing "--config-file"
    (testing "Detects the --config-file option"
      (is (= "some-config.edn"
             (get-in (arguments/parse ["watch" "app"
                                       "--config-file" "some-config.edn"])
                     [:options :config-file]))))
    (testing "Detects the short form -c for config-file"
      (is (= "some-config.edn"
             (get-in (arguments/parse ["watch" "app"
                                       "-c" "some-config.edn"])
                     [:options :config-file]))))
    (testing "Defaults config-file to css-gardener.edn"
      (is (= "css-gardener.edn"
             (-> ["--help"]
                 arguments/parse
                 (get-in [:options :config-file]))))))
  (testing "--config"
    (testing "defaults to nil"
      (is (nil? (-> ["watch" "app"]
                    arguments/parse
                    (get-in [:options :config])))))
    (testing "Returns an error when value is not a valid config map"
      (is (string/includes? (-> ["watch" "app"
                                 "--config" "{:hello :world}"]
                                arguments/parse
                                (get-in [:errors 0]))
                            "Invalid configuration map")))))