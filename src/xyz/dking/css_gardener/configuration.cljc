(ns xyz.dking.css-gardener.configuration
  (:require [xyz.dking.css-gardener.utils.files :as files]
            #?(:clj [clojure.data.json :as json])))

(defn json->map
  "Converts a json string to a Clojure data structure."
  [json]
  #?(:clj (json/read-str json)
     :cljs (js->clj (js/JSON.parse json))))

(defn has-npm-dependency?
  "Determines if a package.json has a dependency on a certain
  library.

  Args:
    package-json: npm configuration as a Clojure map with string keys.
    dependency: Name of the npm package as a string."
  [package-json dependency]
  (boolean (or (get-in package-json ["dependencies" dependency])
               (get-in package-json ["devDependencies" dependency]))))

(defn find-project-configurations
  "Determines which project configuration files exist in the
  current project directory."
  [shadow-cljs-file deps-file lein-file package-json-file]
  (let [shadow-cljs? (files/exists? shadow-cljs-file)
        package-json? (files/exists? package-json-file)
        shadow-cljs-npm-installed?
        (and shadow-cljs? package-json?
             (-> (files/read-file package-json-file)
                 json->map
                 (has-npm-dependency? "shadow-cljs")))
        deps? (files/exists? deps-file)
        lein? (files/exists? lein-file)]
    {:shadow-cljs? shadow-cljs?
     :package-json? package-json?
     :shadow-cljs-npm-installed? shadow-cljs-npm-installed?
     :deps? deps?
     :lein? lein?}))

(defn get-project-configuration-type
  "Determines which project configuration tool should be used to
  invoke css-gardener."
  [{:keys [shadow-cljs?
           package-json?
           shadow-cljs-npm-installed?
           deps?
           lein?] :as configurations}]
  (cond
    (and shadow-cljs? (not shadow-cljs-npm-installed?))
    {:status :failure
     :type nil
     :message (str "Found shadow-cljs.edn, but shadow-cljs was not found "
                   "as a dependency or devDependency in package.json.\n"
                   "css-gardener will not be able to infer the source "
                   "paths for your project. Please invoke css-gardener "
                   "from your Clojure build tool instead.")}

    (and shadow-cljs? shadow-cljs-npm-installed?)
    {:status :success
     :type :shadow-cljs
     :message (str "Found shadow-cljs configuration.")}

    deps?
    {:status :success
     :type :deps
     :message (str "Found tools.deps configuration.")}

    lein?
    {:status :success
     :type :lein
     :message (str "Found Leiningen configuration.")}

    :else
    {:status :failure
     :type nil
     :message (str "Could not find your project configuration, please refer to "
                   "the documentation to learn how to invoke css-gardener.")}))

