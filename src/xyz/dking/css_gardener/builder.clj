(ns xyz.dking.css-gardener.builder)

(defprotocol Builder
  (start [builder])
  (stop [builder])
  (build [builder config])
  (watch [builder config]))

(defmulti
  ^{:doc "Multimethod that constructs a Builder based on the
          configuration values."}
  get-builder :type)

(defmethod get-builder :default
  [{:keys [type]}]
  (let [err (if (nil? type)
              (ex-info "No :type key provided in configuration map."
                       {:type :missing-type})
              (ex-info (str "No builder found for type " type)
                       {:type :no-builder}))]
    (throw err)))
