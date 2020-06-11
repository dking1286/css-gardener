(ns css-gardener.scripts.get-packages
  (:require [clojure.string :as string]
            [css-gardener.scripts.utils :refer [json-parse
                                                slurp]]))

(print (string/join " " (get (json-parse (slurp "lerna.json")) "packages")))