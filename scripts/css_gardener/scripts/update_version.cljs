(ns css-gardener.scripts.update-version
  (:refer-clojure :exclude [exists?])
  (:require [clojure.string :as string]
            [css-gardener.scripts.utils :refer [json-parse
                                                exists?
                                                slurp
                                                spit]]
            [goog.object :as object]
            [xml2js]))

(def ^:private version-regexp
  #"\d+\.\d+\.\d+")
(def ^:private version
  (first (filter #(re-matches version-regexp %) js/process.argv)))
(def ^:private lerna-json
  (json-parse (slurp "lerna.json")))

(when-not version
  (println "Did not recognize version number in command line arguments")
  (js/process.exit 1))

(defmulti update-version
  "Updates the version in the text of a configuration file, calling the
   callback with the new text that should be written to the file."
  (fn [filetype _ _] filetype))

(defmethod update-version :json
  [_ content callback]
  (callback nil (string/replace content
                                #"\"version\": \"\d+\.\d+\.\d+\""
                                (str "\"version\": \"" version "\""))))

(defmethod update-version :xml
  [_ content callback]
  (xml2js/parseString
   content
   (fn [err result]
     (if err
       (callback (ex-info (str "Error while parsing xml: " err) {} err) nil)
       (do
         (aset (-> result (object/get "project") (object/get "version"))
               0 version)
         (callback nil (-> (xml2js/Builder.)
                           (.buildObject result)
                           ;; Add newline at the end of the file
                           (str "\n")
                           ;; Remove the standalone="yes" that is added by
                           ;; xml2js
                           (string/replace #"\s*standalone=\"yes\"\s*" ""))))))))

(doseq [package (get lerna-json "packages")]
  (let [package-json (str package "/package.json")
        pom-xml (str package "/pom.xml")]
    (when (exists? package-json)
      (println (str "Updating version in " package-json " to " version))
      (update-version :json (slurp package-json)
                      (fn [err result]
                        (if err
                          (println (str "Error while updating package.json: "
                                        err))
                          (spit package-json result)))))
    (when (exists? pom-xml)
      (println (str "Updating version in " pom-xml " to " version))
      (update-version :xml (slurp pom-xml)
                      (fn [err result]
                        (if err
                          (println (str "Error while updating pom.xml: "
                                        err))
                          (spit pom-xml result)))))))