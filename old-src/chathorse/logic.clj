(ns chathorse.logic
  (:require [org.httpkit.timer :as timer])
  (:use chathorse.utils)
  (:require [chathorse.response :as response]))


(defn agent [value]
  (let [agent (clojure.core/agent value)]
    (set-error-handler! agent #(.printStackTrace %2))
    agent))

(def ^:private PURGE_TIMEOUT 1000000)
(def ^:private users (atom {}))
(def ^:private waiting-users (agent []))



(def user-connect!)
(def user-save-connection!)
(def user-timeout-connection!)
(def user-post-message!)

(defn- new-user [uid]
  {:uid uid
   :ts (now-ts)
   :conn nil
   :msgs []
   :targets []
   :state nil})


(defn- get-user [uid]
  (let [user (@users uid)]
    
    (when-not user (throw (ex-info (str "No such user: " uid) {:uid uid})))
    user))

(defn- kill-user* [user]
  (user-post-message! (user :uid))
  )


(defn kill-user! [uid]
  (swap! users
         (fn deleter [users uid]
           (if-let [user (users uid)]
             (do
               (send user kill-user*)
               (loop [uids (@user :targets) users (dissoc users uid)]
                 (recur (rest uids) (deleter users (first uids)))))
             users))))

(defn- add-target* [user uid]
  (assoc user :targets (conj (user :targets) uid)))

(defn- connect-users [uids]
  (prn :UIDS-TO-CONNECT uids)
  (assert (< 1 (count uids)))
  (let [users (map get-user uids)]
    (doseq [user users uid uids :when (not= uid (@user :uid))]
      (send user add-target* uid))
    (doseq [user users]
      (user-post-message! (@user :uid) (response/connected)))))

(defn- purge-user* [user]
  (let [ts (user :ts)
        now (now-ts)]
    (if (> now (+ ts PURGE_TIMEOUT))
      (swap! users dissoc (user :uid))
      (timer/schedule-task
       (- (+ ts 1000 PURGE_TIMEOUT) now)
       (send user purge-user*))))
  user)

(defn touch-user* [user]
  (assoc user :ts (now-ts)))
  
(defn- init-user [uid]
  (swap! users
         (fn [users]
           (if (users uid)
             users
             (let [user (agent (new-user uid))]
               
               (send user purge-user*)
               (assoc users uid user))))))



(defn- match-users [queue]
  (if (> 2 (count queue))
    queue
    (let [conn (take 2 queue)
          rest (drop 2 queue)]
      (send waiting-users match-users)
      (connect-users conn)
      rest
      )))

(defn user-connect! [uid params]
  (prn :INITIALIZE-USER uid)
  (init-user uid)
  
  (send waiting-users conj uid)
  (send waiting-users match-users))

(defn- user-respond* [user]
  (let [conn (user :conn)
        msg (first (user :msgs))
        msgs (rest (user :msgs))]

    (if (and msg conn)
      (do
        (conn msg)
        (assoc user :conn nil :msgs msgs))
      user)))

(defn user-save-connection! [uid conn]
  (-> (get-user uid)
      (send (fn [user]
              (when (user :conn)
                (response/connection-stale (user :conn)))
              (assoc user :conn conn)))
      (send user-respond*)
      (send touch-user*)))

(defn user-timeout-connection! [uid conn]
  (-> (get-user uid)
      (send (fn [user]
              (when (= (user :conn) conn)
                (response/connection-stale (user :conn))
                (dissoc user :conn))
              user))))
  

(defn user-post-message! [uid msg]
  (-> (get-user uid)
      (send (fn [user]
              (let [msgs (user :msgs)]
                (assoc user :msgs (conj msgs msg)))))
      (send user-respond*)
      (send touch-user*)))




