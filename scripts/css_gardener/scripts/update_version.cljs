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
(def ^:private update-version-regexp
  #"\^:update-version\s+{:mvn/version \"\d+\.\d+\.\d+\"}")
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

(defmethod update-version :deps
  [_ content callback]
  (callback nil (string/replace content
                                update-version-regexp
                                (str "^:update-version {:mvn/version \"" version "\"}"))))

(defmethod update-version :default
  [filetype _ _]
  (throw (ex-info (str "Unrecognized file type " filetype) {})))

(defn- do-update
  [& {:keys [file-path file-type start-message-fn error-message-fn]}]
  (println (start-message-fn file-path version))
  (update-version file-type (slurp file-path)
                  (fn [err result]
                    (if err
                      (println (error-message-fn err))
                      (spit file-path result)))))

(let [common-dependencies-edn "common-dependencies.edn"]
  (do-update :file-path common-dependencies-edn
             :file-type :deps
             :start-message-fn (constantly "Updating annotated versions in common-dependencies.edn")
             :error-message-fn #(str "Error while updating common-dependencies.edn: " %)))

(doseq [package (get lerna-json "packages")]
  (let [package-json (str package "/package.json")
        pom-xml (str package "/pom.xml")
        deps-edn (str package "/deps.edn")]
    (when (exists? package-json)
      (do-update :file-path package-json
                 :file-type :json
                 :start-message-fn #(str "Updating version in " %1 " to " %2)
                 :error-message-fn #(str "Error while updating package.json: " %)))
    (when (exists? pom-xml)
      (do-update :file-path pom-xml
                 :file-type :xml
                 :start-message-fn #(str "Updating version in " %1 " to " %2)
                 :error-message-fn #(str "Error while updating pom.xml: " %)))
    (when (exists? deps-edn)
      (do-update :file-path deps-edn
                 :file-type :deps
                 :start-message-fn #(str "Updating version in " %1 " to " %2)
                 :error-message-fn #(str "Error while updating deps.edn: " %)))))