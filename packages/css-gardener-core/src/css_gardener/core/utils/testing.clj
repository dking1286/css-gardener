(ns css-gardener.core.utils.testing)

(defmacro with-system
  [[system-sym config-sym] & body]
  `(let [~system-sym (integrant.core/init ~config-sym)]
     ~@body
     (integrant.core/halt! ~system-sym)))

(defmacro deftest-async
  [name & body]
  `(clojure.test/deftest ~name
     (clojure.test/async done#
       (clojure.core.async/go
         ~@body
         (done#)))))