(ns css-gardener.core.utils.testing)

(defmacro with-system
  "Create and starts a system from the passed in config, binding the started
   system to the passed in symbol. Executes the body forms, then stops the
   system."
  [[system-sym config-sym] & body]
  `(let [~system-sym (init-system ~config-sym)]
     ~@body
     (integrant.core/halt! ~system-sym)))

(defn- parse-testing-inputs
  [level-or-message & body]
  (if (string? level-or-message)
    {:level 0
     :message level-or-message
     :body body}
    {:level level-or-message
     :message (first body)
     :body (rest body)}))

(defn- add-nested-levels
  "Adds indentation levels to nested testing forms."
  [level & body]
  (->> body
       (map (fn [form]
              (if (and (list? form)
                       (= 'testing (first form)))
                (cons `testing (cons (inc level) (rest form)))
                form)))))

(defmacro testing
  "Equivalent to the normal \"testing\" macro, except that it prints the
   test case message before running the body.
   
   If calls to this macro are nested, test case messages for the inner call
   will be printed with additional leading whitespace, to ensure that
   the different levels of messages are visually distinct."
  [level-or-message & body]
  (let [{:keys [level message body]} (apply parse-testing-inputs
                                            level-or-message
                                            body)
        body (apply add-nested-levels level body)]
    `(clojure.test/testing ~message
       (println (str (clojure.string/join "" (repeat (inc ~level) "  "))
                     (clojure.string/replace ~message #"\s+" " ")))
       ~@body)))

(comment
  (add-nested-levels
   0
   '(testing "hello world"
      (is (= something something-else))))

  (macroexpand
   '(testing "hello world"
      (testing "goodbye world"
        (is (= something something-else))))))

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