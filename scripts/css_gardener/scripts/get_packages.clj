(ns css-gardener.scripts.get-packages
  (:require [clojure.string :as string]
            [css-gardener.scripts.utils :refer [get-packages]]))

(defn -main
  [& _]
  (print (string/join " " (get-packages))))