(ns xyz.dking.css-gardener.init-test
  (:require [clojure.string :as str]
            [clojure.test :refer :all]
            [xyz.dking.css-gardener.init :as init]
            [xyz.dking.css-gardener.test-helpers :refer [with-fixture
                                                         with-temp-file
                                                         *temp-file*]]
            [xyz.dking.css-gardener.utils.files :as files])
  (:import [java.io BufferedReader StringReader StringWriter]))

(defmacro with-user-input
  [input & body]
  `(binding [*in* (BufferedReader. (StringReader. ~input))
             *out* (StringWriter.)]
     ~@body))

(deftest get-type-from-user-test
  (testing "Prompts the user to choose which type of stylesheet they want."
    (with-user-input "1"
      (init/get-type-from-user!)
      (is (str/includes? (.toString *out*)
                         "Which type of stylesheets"))
      (is (str/includes? (.toString *out*)
                         "Please enter a number:"))))
  (testing "Prompts the user again if they enter an invalid choice."
    (with-user-input "10\n1"
      (init/get-type-from-user!)
      (is (str/includes? (.toString *out*)
                         "Invalid choice"))))
  (testing "Returns the type entered by the user."
    (with-user-input "1"
      (is (= (init/get-type-from-user!) :garden))))
  (testing "Returns the type entered by the user even if they entered a wrong choice first."
    (with-user-input "10\n11\n2"
      (is (= (init/get-type-from-user!) :scss)))))

(deftest initialize-project-test
  (testing "Does nothing when the config file already exists."
    (with-fixture with-temp-file
      (binding [*out* (StringWriter.)]        
        (init/initialize-project {:type :garden
                                  :config-file (.getAbsolutePath *temp-file*)})
        (is (str/includes? (.toString *out*) "already exists")))))
  (testing "Creates the config file if it doesn't already exist."
    (with-fixture with-temp-file
      (.delete *temp-file*) ; Delete the temp file so that it does not exist
      (init/initialize-project {:type :garden
                                :config-file (.getAbsolutePath *temp-file*)})
      (is (files/exists? (.getAbsolutePath *temp-file*)))))
  (testing (str "Asks the user what kind of stylesheet they want if it is not "
                "provided in the config.")
    (with-fixture with-temp-file
      (with-user-input "1"
        (init/initialize-project {:config-file (.getAbsolutePath *temp-file*)})
        (is (str/includes? (.toString *out*) "Which type of stylesheets"))))))
