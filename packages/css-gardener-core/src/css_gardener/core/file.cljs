(ns css-gardener.core.file
  (:require ["path" :as path]
            [clojure.spec.alpha :as s]
            [css-gardener.core.utils.async :as a]))

;; TODO: Make a more robust absolute path spec
(s/def ::absolute-path string?)
(s/def ::content string?)
(s/def ::file (s/keys :req-un [::absolute-path ::content]))

(defn from-path
  "Gets a file map from an absolute or relative path."
  [read-file path]
  (let [absolute-path (path/resolve path)]
    (->> (read-file absolute-path)
         (a/map (fn [content] {:absolute-path absolute-path
                               :content content})))))