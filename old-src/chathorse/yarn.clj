(ns chathorse.yarn
  (:use [chathorse.mongo]
        [monger.operators])
  (:require [monger.collection :as mongo]))


(defn get-new-yarn []
  (mongo/insert-and-return YARNS {}))

(defn get-last-messages
  ([yarn-id limit]
     (mongo/find-maps MESSAGES { :yarn-id yarn-id}))
  ([yarn-id]
     (get-last-messages yarn-id 100)))


  
(defn get-newer-messages
  ([yarn-id last-id limit]
     (mongo/find-maps MESSAGES {:_id {$gt last-id} :yarn-id yarn-id}))
  ([yarn-id last-id ]
     (get-newer-messages yarn-id last-id 100)))



(defn post-message
  [yarn-id user-id content]
  (mongo/insert MESSAGES {:yarn-id yarn-id :user-id user-id :content content}))




     
  
  
  