(ns css-gardener.scripts.update-version
  (:require [clojure.string :as string]
            [clojure.tools.cli :refer [parse-opts]]
            [css-gardener.scripts.utils :refer [exists?
                                                get-packages
                                                xml-parse
                                                xml-serialize]]))

(def ^:private update-version-regexp
  #"\^:update-version\s+\{:mvn/version \"\d+\.\d+\.\d+\"\}")

(defn- parse-version
  [version]
  (re-matches #"^\d+\.\d+\.\d+$" version))

(def ^:private cli-options
  [["-v" "--version VERSION" "New version number to update to"
    :parse-fn parse-version
    :validate [(complement nil?)
               "--version must be a version string of the form X.X.X"]]])

(defmulti ^:private update-version
  "Updates the version in the text of a configuration file, returning
   the new text that should be written to the file."
  (fn [filetype _ _] filetype))

(defmethod update-version :json
  [_ content version]
  (string/replace content #"\"version\": \"\d+\.\d+\.\d+\""
                  (str "\"version\": \"" version "\"")))

(defmethod update-version :xml
  [_ xml-str version]
  (-> (xml-parse xml-str)
      ;; For some reason the serialization function has trouble with the attrs
      ;; on the "project" tag. Remove them, and then put them back into the
      ;; string later.
      (assoc :attrs {})
      ;; Update the "version" tag
      (update :content (fn [content]
                         (map (fn [el]
                                (if (= (:tag el) :version)
                                  (assoc el :content (list version))
                                  el))
                              content)))
      ;; Serialize back to xml string
      xml-serialize
      ;; Put back the attributes on the project tag
      (string/replace "<project>"
                      "\n<project xmlns=\"http://maven.apache.org/POM/4.0.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">")))

(defmethod update-version :deps
  [_ content version]
  (string/replace content
                  update-version-regexp
                  (str "^:update-version {:mvn/version \"" version "\"}")))

(defmethod update-version :default
  [filetype _ _]
  (throw (ex-info "Unrecognized file type." {:type filetype})))

(defn- do-update
  [version & {:keys [file-path file-type start-message-fn]}]
  (println (start-message-fn file-path version))
  (let [updated (update-version file-type (slurp file-path) version)]
    (spit file-path updated)))

(defn -main
  "Main function."
  [& args]
  (let [{:keys [options errors]} (parse-opts args cli-options)]
    (when errors
      (println (first errors))
      (System/exit 1))
    (let [{:keys [version]} options]
      (doseq [package (get-packages)]
        (let [package-json (str package "/package.json")
              pom-xml (str package "/pom.xml")
              deps-edn (str package "/deps.edn")]
          (when (exists? package-json)
            (do-update version
                       :file-path package-json
                       :file-type :json
                       :start-message-fn #(str "Updating version in " %1 " to " %2)))
          (when (exists? pom-xml)
            (do-update version
                       :file-path pom-xml
                       :file-type :xml
                       :start-message-fn #(str "Updating version in " %1 " to " %2)))
          (when (exists? deps-edn)
            (do-update version
                       :file-path deps-edn
                       :file-type :deps
                       :start-message-fn #(str "Updating version in " %1 " to " %2))))))))

(comment
  (-> (xml-parse (slurp "packages/common/pom.xml"))
      (assoc :attrs {})
      xml-serialize
      (string/replace "<project>" "<project xmlns=\"http://maven.apache.org/POM/4.0.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">"))
  (update-version :xml (slurp "packages/common/pom.xml") "1.3.4")
  (update-version :json (slurp "packages/core/package.json") "1.3.4")
  (update-version :deps (slurp "common-dependencies.edn") "1.3.4"))