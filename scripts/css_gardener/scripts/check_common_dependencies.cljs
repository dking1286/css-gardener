(ns css-gardener.scripts.check-common-dependencies
  (:refer-clojure :exclude [exists?])
  (:require [css-gardener.scripts.utils :refer [edn-parse
                                                exists?
                                                json-parse
                                                slurp]]))

(defn- mismatch?
  [expected-version version]
  (not (= version expected-version)))

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

(defmulti ^:private check-package-config
  (fn [_ _ config-type _] config-type))

(defmethod check-package-config :package-json
  [common-dependencies package-name _ config]
  (doseq [[name version] (merge (get config "dependencies")
                                (get config "devDependencies"))]
    (let [expected-version (get-in common-dependencies [:npm name])]
      (when (mismatch? expected-version version)
        (throw (js/Error. (str "Package " package-name " has version " version
                               " of dependency " name ", expected "
                               expected-version)))))))

(defmethod check-package-config :shadow-cljs
  [common-dependencies package-name _ config]
  (doseq [[name version] (:dependencies config)]
    (let [expected-version (get-in common-dependencies
                                   [:cljs name :mvn/version])]
      (when (mismatch? expected-version version)
        (throw (js/Error. (str "Package " package-name " has version " version
                               " of dependency " name ", expected "
                               expected-version)))))))

(defmethod check-package-config :deps-edn
  [common-dependencies package-name _ config]
  (let [all-deps (concat (:deps config)
                         (mapcat (fn [[_ v]] (:deps v)) (:aliases config))
                         (mapcat (fn [[_ v]] (:extra-deps v)) (:aliases config)))]
    (doseq [[name version] all-deps]
      (let [expected-version (get-in common-dependencies [:cljs name])]
        (when (mismatch? expected-version version)
          (throw (js/Error. (str "Package " package-name " has version " version
                                 " of dependency " name ", expected "
                                 expected-version))))))))

(defmethod check-package-config :default
  [_ _ config-type _]
  (throw (js/Error. (str "Unknown config type " config-type))))

(defn- check-package-configs
  [common-dependencies package-configs]
  (doseq [[package-name configs] package-configs
          [config-type config] configs]
    (check-package-config
     common-dependencies package-name config-type config)))

(def ^:private lerna-json
  (json-parse (slurp "lerna.json")))

(def ^:private common-dependencies
  (edn-parse (slurp "common-dependencies.edn")))

(def ^:private package-configs
  (->> (get lerna-json "packages")
       (map #(vector % (read-package-configs %)))
       (into {})))

(try
  (check-package-configs common-dependencies package-configs)
  (println "Common dependencies are in sync!")
  (catch js/Error err
    (println (str "Error: " (.-message err)))
    (js/process.exit 1)))