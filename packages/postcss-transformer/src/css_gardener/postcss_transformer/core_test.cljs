(ns css-gardener.postcss-transformer.core-test
  (:require [clojure.test :refer [deftest async testing is]]
            [css-gardener.common.js :refer [to-js]]
            [css-gardener.postcss-transformer.core :refer [enter exit]]
            [fs]
            [goog.object :as gobj]
            [path]))

(defn- test-stylesheet-path
  [name]
  (path/resolve (str "src/css_gardener/postcss_transformer/"
                     "test_stylesheets/"
                     name)))

(defn- test-stylesheet
  [name]
  (let [abs-path (test-stylesheet-path name)]
    #js {:absolutePath abs-path
         :content (fs/readFileSync abs-path "utf8")}))

(deftest t-enter-no-absolute-path
  (testing "yields an error when no 'absolutePath' key exists on the file"
    (async done
      (enter #js {:content "hello world"}
             #js {}
             (fn [err result]
               (is (nil? result))
               (is (= "'absolutePath' key is missing on the input file"
                      (.-message err)))
               (done))))))

(deftest t-enter-no-content
  (testing "yields an error when no 'content' key exists on the file"
    (async done
      (enter #js {:absolutePath "/hello/world"}
             #js {}
             (fn [err result]
               (is (nil? result))
               (is (= "'content' key is missing on the input file"
                      (.-message err)))
               (done))))))

(deftest t-enter-applies-postcss-plugins-to-input-file
  (testing "applies plugins to input file"
    (async done
      (enter (test-stylesheet "flex.css")
             (to-js {:plugins [{:node-module "cssnano"}]})
             (fn [err result]
               (is (nil? err))
               (is (= "/*! comment */.container{flex:1}"
                      (gobj/get result "content")))
               (done))))))

(deftest t-enter-applies-options-passed-to-plugins
  (testing "applies options passed to plugins"
    (async done
      (enter (test-stylesheet "flex.css")
             (to-js {:plugins [{:node-module "cssnano"
                                :options {:preset ["default" {:discard-comments {:remove-all true}}]}}]})
             (fn [err result]
               (is (nil? err))
               (is (= ".container{flex:1}"
                      (gobj/get result "content")))
               (done))))))

(deftest t-exit-yields-file-unmodified
  (testing "yields the input file unmodified"
    (async done
      (exit #js {:absolutePath "/hello/world"
                 :content "hello world"}
            #js {}
            (fn [err result]
              (is (nil? err))
              (is (gobj/equals #js {:absolutePath "/hello/world"
                                    :content "hello world"}
                               result))
              (done))))))