(ns css-gardener.core.system
  (:require [css-gardener.core.cljs-parsing :as cljs]
            [css-gardener.core.dependency :as dependency]
            [css-gardener.core.logging :as logging]
            [css-gardener.core.modules :as modules]
            [css-gardener.core.utils.fs :as fs]
            [integrant.core :as ig]))

(def config
  {::logging/logger {:level :info
                     :sinks #{:console}}

   ::modules/load {}

   ::fs/exists? {}
   ::fs/read-file {}

   ::cljs/deps {:exists? (ig/ref ::fs/exists?)}
   ::dependency/deps {:logger (ig/ref ::logging/logger)
                      :load-module (ig/ref ::modules/load)
                      :cljs-deps (ig/ref ::cljs/deps)}})