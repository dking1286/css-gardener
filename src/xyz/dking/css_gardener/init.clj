(ns xyz.dking.css-gardener.init
  (:require [clojure.string :as str]
            [xyz.dking.css-gardener.config :as config]
            [xyz.dking.css-gardener.utils.files :as files]
            [xyz.dking.css-gardener.logging :as logging]))

(def type-choices [:garden :scss :css])

(def which-type-message
  (->> type-choices
       (map-indexed (fn [i type] (str (inc i) ". " (name type))))
       (into ["Which type of stylesheets do you want to use for your project?"])
       (str/join "\n")))

(defn parse-number-input
  "Parses a string into an integer. Returns nil if the
  string does not represent a valid integer."
  [input]
  (try
    (Integer/parseUnsignedInt input)
    (catch NumberFormatException e
      nil)))

(defn get-type-from-user!
  "Prompts the user to enter the type of stylesheet they want to use."
  []
  (println which-type-message)
  (loop []
    (print "Please enter a number: ")
    (flush)
    (let [input (parse-number-input (read-line))
          choice (get type-choices (dec input))]
      (if choice
        choice
        (do (println "Invalid choice, please enter one of the options above.")
            (recur))))))

(defn initialize-project
  "Initializes a project to use css-gardener."
  [config]
  (let [type (or (:type config) (get-type-from-user!))
        config-file (or (:config-file config) config/default-config-file)]
    (if (files/exists? config-file)
      (logging/info (str "Did not create configuration file "
                         config-file
                         ", file already exists."))
      (let [config-map (config/default-config type)]
        (logging/info (str "Creating configuration file " config-file "."))
        (spit config-file (str/replace (pr-str config-map) ", " "\n "))))))

