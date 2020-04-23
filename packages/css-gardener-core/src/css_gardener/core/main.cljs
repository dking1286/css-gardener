(ns css-gardener.core.main)

(defn main
  [& _]
  (println "hello world"))

(defn do-stuff
  []
  (let [transformer (js/require "@css-gardener/css-transformer")]
    (-> (.enter transformer "hello")
        (.then (fn [result] (println result)))
        (.then (fn [_] (js/process.exit 0))))))

(comment
  (do-stuff))