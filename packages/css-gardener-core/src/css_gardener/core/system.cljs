(ns css-gardener.core.system
  (:require [css-gardener.core.change-detection :as changes]
            [css-gardener.core.cljs-parsing :as cljs]
            [css-gardener.core.dependency :as dependency]
            [css-gardener.core.logging :as logging]
            [css-gardener.core.modules :as modules]
            [css-gardener.core.utils.fs :as fs]
            [integrant.core :as ig]))

(def ^{:doc "Default Integrant system configuration."}
  config
  {::logging/logger {:level :info
                     :sinks #{:console}}

   ::modules/load {}

   ::fs/exists? {}
   ::fs/read-file {}

   ::cljs/deps {:exists? (ig/ref ::fs/exists?)}
   ::dependency/deps {:logger (ig/ref ::logging/logger)
                      :load-module (ig/ref ::modules/load)
                      :cljs-deps (ig/ref ::cljs/deps)}
   ::dependency/deps-graph {:logger (ig/ref ::logging/logger)
                            :exists? (ig/ref ::fs/exists?)
                            :read-file (ig/ref ::fs/read-file)
                            :deps (ig/ref ::dependency/deps)}

   ::changes/input-channel {}
   ::changes/watcher {:source-paths []
                      :input-channel (ig/ref ::changes/input-channel)}})