(ns chathorse.room
  (:use chathorse.utils
        [chathorse.connect :only [leave-message!]]))


(def rooms (atom {}))
(def users (atom {}))
(def user-queue (agent []))

(defn init-room [id users]
  {:id id
   :users users})

(defn create-room! [users]
  (let [id (uniq-id)]
    (swap! rooms assoc id (init-room id users))
    (doseq [user users]
      (swap! users assoc-in [user :room] id)
      (leave-message! user {:room id}))))

(defn match-users! [queue]
  (if (<= 1 (count queue))
    queue
    (do
      (create-room! (take 2 queue))
      (drop 2 queue))))

(defn enqueue-user! [queue uid]
  (send user-queue match-users!) ; Init matching users.
  (conj queue uid))
  

(defn init-user [uid params]
  {:id uid
   :ts (now-ts)
   :params params
   :room nil})


(defn new-user-waiting! [uid params]
  (when (@users uid)
    (throw (ex-info "User already subscribed!" {:uid uid :old (@users uid)})))
  (swap! users assoc uid (init-user uid params))
  (send user-queue enqueue-user! uid))


