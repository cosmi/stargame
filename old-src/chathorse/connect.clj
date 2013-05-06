(ns chathorse.connect
  (:use chathorse.utils)
  (:require fs.core))



(def mailboxes (atom {}))


(defn- init-mailbox [id]
  {:id id
   :last-ts (now-ts)
   :conn nil
   :msg nil})

(defn- get-mailbox [id]
  (or (get @mailboxes id)
      (let [r (ref (init-mailbox id))]
        (swap! mailboxes assoc id r)
        r)))


(defn leave-message! [id value]
  (when-let [conn
             (let [mailbox (get-mailbox id)]
               (prn :> mailbox)
               (dosync
                (ensure mailbox)
                (if-let [conn (take-from! mailbox :conn)]
                  conn
                  (do
                    (prn "No conn, leaving msg")
                    (store-at! mailbox :msg value)
                    nil))))]
    (try
      (conn value)
      (catch Throwable t
        (leave-message! id value)))))

(defn save-connection! [id conn]
  (when-let [msg
             (let [mailbox (get-mailbox id)]
               (prn :> mailbox conn)

               (dosync
                (ensure mailbox)
                (if-let [msg (take-from! mailbox :msg)]
                  msg
                  (do
                    (store-at! mailbox :conn conn)
                    nil))))]
    (conn msg)))



(defn delete-box! [id]
  (let [mailbox (get-mailbox id)]
    (dosync
     (ensure mailbox)
     (if-let [conn (take-from! mailbox :conn)]
       conn
       (do
         (prn "No conn, leaving msg")
         (store-at! mailbox :msg value)
       nil)))))