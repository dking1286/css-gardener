(ns css-gardener.scripts.check-common-dependencies
  (:require [clojure.pprint :refer [pprint]]
            [css-gardener.scripts.utils :refer [edn-parse
                                                exists?
                                                get-packages
                                                json-parse]]))

(defn- mismatch?
  [expected-version version]
  (if (set? expected-version)
    (not (contains? expected-version version))
    (not (= version expected-version))))

(defn- read-package-configs
  [package]
  {:package-json
   (let [package-json-name (str package "/package.json")]
     (when (exists? package-json-name)
       (json-parse (slurp package-json-name))))

   :shadow-cljs
   (let [shadow-cljs-name (str package "/shadow-cljs.edn")]
     (when (exists? shadow-cljs-name)
       (edn-parse (slurp shadow-cljs-name))))

   :deps-edn
   (let [deps-edn-name (str package "/deps.edn")]
     (when (exists? deps-edn-name)
       (edn-parse (slurp deps-edn-name))))})

(defn- check-version
  [package-name common-deps [name version]]
  (let [expected-version (get common-deps name)]
    (when (mismatch? expected-version version)
      {:package-name package-name
       :dep-name name
       :expected-version expected-version
       :actual-version version})))

(defn- check-versions
  [package-name common-deps deps]
  (->> deps
       (map #(check-version package-name common-deps %))
       (filter (complement nil?))))

(defmulti ^:private check-package-config
  (fn [_ _ config-type _] config-type))

(defmethod check-package-config :package-json
  [common-dependencies package-name _ config]
  (let [common-deps (:npm common-dependencies)
        deps (merge (get config "dependencies") (get config "devDependencies"))]
    (check-versions package-name common-deps deps)))

(defmethod check-package-config :shadow-cljs
  [common-dependencies package-name _ config]
  (let [common-deps (->> (:cljs common-dependencies)
                         (map (fn [[name {:keys [mvn/version]}]]
                                [name version]))
                         (into {}))
        deps (into {} (:dependencies config))]
    (check-versions package-name common-deps deps)))

(defmethod check-package-config :deps-edn
  [common-dependencies package-name _ config]
  (let [common-deps (:cljs common-dependencies)
        deps (into {} (concat (:deps config)
                              (mapcat (fn [[_ v]] (:deps v))
                                      (:aliases config))
                              (mapcat (fn [[_ v]] (:extra-deps v))
                                      (:aliases config))
                              (mapcat (fn [[_ v]] (:override-deps v))
                                      (:aliases config))))]
    (check-versions package-name common-deps deps)))

(defmethod check-package-config :default
  [_ _ config-type _]
  (throw (ex-info "Unknown config type" {:type config-type})))

(defn- check-package-configs
  [common-dependencies package-configs]
  (->> package-configs
       (mapcat (fn [[package-name configs]]
                 (->> configs
                      (mapcat (fn [[config-type config]]
                                (check-package-config common-dependencies
                                                      package-name
                                                      config-type
                                                      config))))))))

(defn -main
  [& _]
  (let [common-dependencies (edn-parse (slurp "common-dependencies.edn"))
        package-configs (->> (get-packages)
                             (map #(vector % (read-package-configs %)))
                             (into {}))
        errors (check-package-configs common-dependencies package-configs)]
    (if (seq errors)
      (do
        (println "Common dependencies are not in sync, found the following issues:")
        (pprint errors)
        (System/exit 1))
      (do
        (println "Common dependencies are in sync!")
        (System/exit 0)))))
