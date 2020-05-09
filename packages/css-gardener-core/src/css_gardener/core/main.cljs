(ns css-gardener.core.main
  (:require [css-gardener.core.arguments :as arguments]
            [css-gardener.core.cljs-parsing]
            [css-gardener.core.config]
            [css-gardener.core.dependency]
            [css-gardener.core.system]))

(defn main
  []
  (let [args (.slice js/process.argv 2)
        {:keys [errors options summary]} (arguments/parse args)]
    (cond
      (:help options) (println summary)
      errors (println (first errors))
      :else (println "hello world"))))
