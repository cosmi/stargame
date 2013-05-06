(ns chathorse.mongo
  (:require [monger.core :as mg])
  (:import [com.mongodb MongoOptions ServerAddress]
           [org.bson.types ObjectId]))



(defn init-mongo []
  (mg/connect!)
  (mg/set-db! (mg/get-db "chathorse-dev")))

(defn object-id
  ([] (ObjectId.))
  ([s] (ObjectId. s)))

(def YARNS :yarns)
(def MESSAGES :messages)

