(ns css-gardener.core.system
  (:require [css-gardener.core.arguments :as arguments]
            [css-gardener.core.caching :as caching]
            [css-gardener.core.change-detection :as changes]
            [css-gardener.core.cljs-parsing :as cljs]
            [css-gardener.core.config :as config]
            [css-gardener.core.dependency :as dependency]
            [css-gardener.core.logging :as logging]
            [css-gardener.core.modules :as modules]
            [css-gardener.core.output :as output]
            [css-gardener.core.transformation :as transformation]
            [css-gardener.core.utils.fs :as fs]
            [integrant.core :as ig]))

(def ^{:doc "Default Integrant system configuration."}
  config
  {::config/config
   {:source-paths []
    :builds {}
    :rules {}}

   ::arguments/command :compile

   ::arguments/build-id {:config (ig/ref ::config/config)}

   ::logging/logger
   {:level :info
    :sinks #{:console}}

   ::modules/load
   {}

   ::fs/exists?
   {}

   ::fs/read-file
   {}

   ::fs/write-file
   {}

   ::cljs/deps
   {:exists? (ig/ref ::fs/exists?)
    :logger (ig/ref ::logging/logger)}

   ::dependency/resolvers
   {:config (ig/ref ::config/config)
    :logger (ig/ref ::logging/logger)
    :load-module (ig/ref ::modules/load)}

   ::dependency/deps
   {:config (ig/ref ::config/config)
    :logger (ig/ref ::logging/logger)
    :resolvers (ig/ref ::dependency/resolvers)
    :cljs-deps (ig/ref ::cljs/deps)}

   ::dependency/deps-graph
   {:config (ig/ref ::config/config)
    :logger (ig/ref ::logging/logger)
    :exists? (ig/ref ::fs/exists?)
    :read-file (ig/ref ::fs/read-file)
    :deps (ig/ref ::dependency/deps)}

   ::caching/compilation-cache
   {}

   ::caching/dependency-graph-cache
   {}

   ::transformation/transformers
   {:config (ig/ref ::config/config)
    :logger (ig/ref ::logging/logger)
    :load-module (ig/ref ::modules/load)}

   ::transformation/transform
   {:config (ig/ref ::config/config)
    :transformers (ig/ref ::transformation/transformers)}
   
   ::transformation/postprocess
   {:config (ig/ref ::config/config)
    :transformers (ig/ref ::transformation/transformers)}

   ::transformation/compile-all
   {:logger (ig/ref ::logging/logger)
    :config (ig/ref ::config/config)
    :compilation-cache (ig/ref ::caching/compilation-cache)
    :read-file (ig/ref ::fs/read-file)
    :transform (ig/ref ::transformation/transform)
    :postprocess (ig/ref ::transformation/postprocess)}

   ::changes/input-channel
   {}

   ::changes/watcher
   {:watch? false
    :logger (ig/ref ::logging/logger)
    :config (ig/ref ::config/config)
    :input-channel (ig/ref ::changes/input-channel)}

   ::output/output-channel
   {}})