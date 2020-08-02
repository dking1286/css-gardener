(ns css-gardener.scripts.get-packages
  (:require [clojure.string :as string]
            [css-gardener.scripts.utils :refer [get-packages]]))

(print (string/join " " (get-packages)))