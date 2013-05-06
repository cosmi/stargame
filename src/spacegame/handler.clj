(ns spacegame.handler
  (:use compojure.core
        spacegame.utils
        clojure.pprint
        ;[chathorse.boxes]
        ;[chathorse.topics :only [get-topic topics-admin-view topics-admin-change! censor-admin-view censor-admin-change! apply-censor]]
        [org.httpkit.server]
        [spacegame.config :only [config LANGS]]
        ring.middleware.http-basic-auth)
  ;(:import  [org.httpkit.server IListenableFuture])
  (:require [org.httpkit.timer :as timer])
  (:require [compojure.handler :as handler]
            [compojure.route :as route])
  (:require [ring.util.response :as response]
            [clojure.data.json :as json])
  (:require [org.httpkit.server :as server]))




(def LISTENER-TIMEOUT 30000)
(def HALVEN-THRESHOLD-TIMEOUT 5000.)
(def DISCO-TIMEOUT 50000)
(def MAX-MSG-LEN 255)


(defn net->clj [source]
  (json/read-str source :key-fn keyword))
(defn clj->net [%]
  (json/write-str %))


(defn authenticate [username password]
  (if (and (= username (config :admin-name))
           (= password (config :admin-pass)))
    {:username username}))


(defroutes app-routes
  (context "/admin" []
    (wrap-require-auth (constantly nil) authenticate
                       "The Secret Area" {:body "You're not allowed in The Secret Area!"}))

  (GET "/"  [] (response/resource-response "index.html" {:root "public"}))
  
  (route/resources "/")
  
  (route/not-found "Not Found"))

(def app
    (handler/site app-routes))

