(ns css-gardener.core.system
  (:require [css-gardener.core.cljs-parsing]
            [css-gardener.core.dependency]
            [css-gardener.core.modules]
            [css-gardener.core.utils.fs]
            [integrant.core :as ig]))

(def config
  {:load-module nil
   :fs nil
   :cljs-deps {:fs (ig/ref :fs)}
   :deps {:load-module (ig/ref :load-module)
          :cljs-deps (ig/ref :cljs-deps)}})