(ns chathorse.response
  (:use chathorse.utils))


(defn connection-stale [connection]
  (connection "retry"))



(defn connected []
  "connected!")

  