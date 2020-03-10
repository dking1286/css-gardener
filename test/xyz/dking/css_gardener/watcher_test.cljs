(ns xyz.dking.css-gardener.watcher-test
  (:require [clojure.core.async :refer [go <! alts! timeout]]
            [clojure.test :refer [deftest testing is async]]
            [com.stuartsierra.component :as component]
            [xyz.dking.css-gardener.watcher :refer [watch
                                                    new-stub-watcher
                                                    trigger-change!
                                                    new-file-system-watcher]]))

(deftest stub-watcher-no-emission
  (testing "emits nothing when no change has been triggered manually"
    (async done
      (go
        (let [watcher (component/start (new-stub-watcher))
              changes (watch watcher "./src")]
          (is (= ["NOTHING" :default]
                 (alts! [changes] :default "NOTHING")))
          (component/stop watcher)
          (done))))))

(deftest stub-watcher-watch-emits-file
  (testing "emits the absolute path to a file when a change is triggered manually"
    (async done
      (go
        (let [watcher (component/start (new-stub-watcher))
              changes (watch watcher "./src")]
          (trigger-change! watcher "./src/some_file")
          (is (= ["./src/some_file" changes]
                 (alts! [changes (timeout 1000)])))
          (component/stop watcher)
          (done))))))

(deftest stub-watcher-watch-emits-only-one-file
  (async done
    (go
      (let [watcher (component/start (new-stub-watcher))
            changes (watch watcher "./src")]
        (trigger-change! watcher "./src/some_file")
        (<! changes)
        (is (= ["NOTHING" :default]
               (alts! [changes] :default "NOTHING")))
        (component/stop watcher)
        (done)))))

(deftest file-system-watcher-watch-called-before-start
  (let [w (new-file-system-watcher)]
    (is (thrown? js/Error (watch w "./src")))))

(deftest file-system-watcher-no-changes
  (async done
    (go
      (let [w (component/start (new-file-system-watcher))
            changes (watch w "./src")]
        (is (= ["NOTHING" :default]
               (alts! [changes] :default "NOTHING")))
        (component/stop w)
        (done)))))

(deftest file-system-watcher-one-change
  (async done
    (go
      (let [w (component/start (new-file-system-watcher))]
        ;; FINISH ME
        (done)))))