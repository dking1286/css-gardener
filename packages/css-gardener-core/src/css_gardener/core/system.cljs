(ns css-gardener.core.system
  (:require [css-gardener.core.cljs-parsing :as cljs]
            [css-gardener.core.dependency :as dependency]
            [css-gardener.core.modules :as modules]
            [css-gardener.core.utils.fs :as fs]
            [integrant.core :as ig]))

(def config
  {::modules/load {}

   ::fs/exists? {}
   ::fs/read-file {}

   ::cljs/deps {:exists? (ig/ref ::fs/exists?)}
   ::dependency/deps {:load-module (ig/ref ::modules/load)
                      :cljs-deps (ig/ref ::cljs/deps)}})