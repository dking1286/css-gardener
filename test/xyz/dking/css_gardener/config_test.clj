(ns xyz.dking.css-gardener.config-test
  (:require [clojure.edn :as edn]
            [clojure.test :refer :all]
            [xyz.dking.css-gardener.config :as config]
            [xyz.dking.css-gardener.test-helpers :refer [*temp-file*
                                                         with-temp-file]]))

(use-fixtures :each with-temp-file)

(deftest from-file-test
  (testing "config file not found"
    (let [result (config/from-file "i-do-not-exist")]
      (is (= :failure (:status result)))
      (is (= :config-file-not-found (:reason result)))))
  (testing "config file does not contain edn"
    (spit *temp-file* "{\"hello\": \"world\"}")
    (let [result (config/from-file *temp-file*)]
      (is (= :failure (:status result)))
      (is (= :config-file-invalid (:reason result)))))
  (testing "config file valid"
    (spit *temp-file* "{:type :garden}")
    (let [result (config/from-file *temp-file*)]
      (is (= :success (:status result)))
      (is (= {:type :garden} (:result result))))))

(deftest from-cli-args-test
  (testing "parsing cli args"
    (let [result (config/from-cli-args ["-h"])]
      (is (true? (:help result)))))
  (testing "parsing type"
    (let [result (config/from-cli-args ["-t" "garden"])]
      (is (= :garden (:type result)))))
  (testing "parsing input-files"
    (let [result (config/from-cli-args ["-i" "hello.clj,world.clj"])]
      (is (= ["hello.clj" "world.clj"]
             (:input-files result)))))
  (testing "missing input-files"
    (let [result (config/from-cli-args ["-i"])]
      (is (= nil (:input-files result)))))
  (testing "empty input-files"
    (let [result (config/from-cli-args ["-i" ""])]
      (is (= [] (:input-files result))))))
