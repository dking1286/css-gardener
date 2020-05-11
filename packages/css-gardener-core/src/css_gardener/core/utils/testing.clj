(ns css-gardener.core.utils.testing)

(defmacro with-system
  "Create and starts a system from the passed in config, binding the started
   system to the passed in symbol. Executes the body forms, then stops the
   system."
  [[system-sym config-sym] & body]
  `(let [~system-sym (integrant.core/init ~config-sym)]
     ~@body
     (integrant.core/halt! ~system-sym)))

(defmacro deftest-async
  "Macro to define an asynchronous test. Automatically wraps the body forms in
   (async ...) and (go ...) so that <! and >! can be used in the test without
   needing to write this boilerplate for every test case."
  [name & body]
  `(clojure.test/deftest ~name
     (clojure.test/async done#
       (clojure.core.async/go
         ~@body
         (done#)))))