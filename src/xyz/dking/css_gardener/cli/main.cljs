(ns xyz.dking.css-gardener.cli.main
  (:require ["child_process" :as cp]
            ["fs" :as fs]
            ["which" :as which]
            [clojure.string :as str]
            [xyz.dking.css-gardener.configuration
             :refer [find-project-configurations
                     get-project-configuration-type]]))

(goog-define RUN_MAIN false)

(def success-status 0)

(defn is-windows?
  "Determines if the user is on Windows."
  []
  (str/includes? js/process.platform "win32"))

(defn run-child-process
  "Runs a child process synchronously, connecting stdio of the
  child process to the parent process."
  ([working-dir command args]
   (run-child-process working-dir command args {}))
  ([working-dir command args process-opts]
   (let [opts (-> {:cwd working-dir :stdio "inherit"}
                  (merge process-opts)
                  clj->js)
         executable (which/sync command #js {:nothrow true})]
     (if-not executable
       (throw (ex-info (str "Executable '"
                            command
                            "' not found on system path.")
                       {:command command :args args}))
       (cp/spawnSync executable (into-array args) opts)))))

(defn get-output-from-process
  "Runs a child process synchronously, and returns the data written to
  stdout as a string.

  If the process returns a non-success status, an exception is thrown
  with the data written to stderr."
  [working-dir command args]
  (let [out (run-child-process working-dir command args {:stdio "pipe"})]
    (if (not= (.-status out) success-status)
      (throw (ex-info (str "Child process " command " failed.")
                      {:stderr (.toString (.-stderr out))}))
      (str/trim-newline (.toString (.-stdout out))))))

(defn project-classpath
  "Gets the classpath of the current project from the project management
  tool corresponding to the project type."
  [project-type]
  (case project-type
    :shadow-cljs
    (get-output-from-process "." "npx" ["shadow-cljs" "classpath"])

    :deps
    (get-output-from-process "." "clojure" ["-Spath"])

    :lein
    (get-output-from-process "." "lein" ["classpath"])

    ""))

(def nonempty? (complement empty?))

(defn make-classpath
  "Constructs the classpath for invoking css-gardener through the
  java executable.

  TODO: This may need to determine the path to the lib jars, since
  it (may?) not be at the top level of node_modules."
  [project-classpath]
  (let [separator (if (is-windows?) ";" ":")
        paths ["node_modules/css-gardener/src"
               "node_modules/css-gardener/target/java-deps/*"
               project-classpath]]
    (->> paths
         (filter nonempty?)
         (str/join separator))))

(defn make-java-args
  "Constructs the java arguments that should be used to invoke
  css-gardener."
  [classpath & args]
  (concat ["-cp" classpath
           "clojure.main"
           "-m" "xyz.dking.css-gardener.main"]
          args))

(defn run-css-gardener-java
  "Invokes the css-gardener Clojure code with the Java executable.

  Uses the configuration of the current project to determine the
  JVM classpath.

  Any arguments are passed as command line arguments to css-gardener."
  [& args]
  (let [{:keys [status type message]}
        (-> (find-project-configurations "shadow-cljs.edn"
                                         "deps.edn"
                                         "project.clj"
                                         "package.json")
            (get-project-configuration-type))]
    (if (not= status :success)
      (do
        (println message)
        (js/process.exit 1))
      (let [project-classpath (project-classpath type)
            classpath (make-classpath project-classpath)
            java-args (apply make-java-args classpath args)]
        (run-child-process "." "java" java-args)))))

(defn ^:dev/before-load before-load
  []
  (print "Reloading..."))

(defn ^:dev/after-load after-load
  []
  (println "done!"))

(defn main
  [& args]
  (if RUN_MAIN
    (apply run-css-gardener-java args)
    (println "Node repl started!")))

