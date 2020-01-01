(ns xyz.dking.css-gardener.builder)

(defprotocol Builder
  (start [builder])
  (stop [builder])
  (build [builder config])
  (watch [builder config]))

