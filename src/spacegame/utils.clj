(ns spacegame.utils
  (:import [org.bson.types ObjectId])
  )

(defn now [] (java.util.Date.))
(defn now-ts [] (System/currentTimeMillis))

(defn store-at! "If a-ref contains a map, stores a-ref value under id"
  [a-ref id value]
  (dosync
   (ensure a-ref)
   (if (@a-ref id)
     (throw (ex-info "Already stored." {:id id :value (@a-ref id)}))
     (alter a-ref assoc id value))))

(defn take-from!
  "If a-ref contains a map, returns value under id"
  [a-ref id]
  (dosync
   (ensure a-ref)
   (when-let [value (@a-ref id)]
     (alter a-ref dissoc id)
     value)))

(defn uniq-id []
  (str (ObjectId.)))

(defn safe-read-string [s]
  (binding [*read-eval* false] (read-string s)))


(defmacro asrt-> [obj & tests]
  `(let [obj# ~obj]
     (if-not (-> obj# ~@tests)
       (throw (Exception. (prn-str "Invalid data:" obj#)))
       obj#)))

(defn to-kw [obj]
  (if (keyword? obj) obj (-> obj name keyword)))


(defn is-id? [val]
  (or (and (string? val) (-> val count (<= 40))) (integer? val)))

(defn safe-agent [val]
  (doto (agent val)
    (set-error-handler! (fn [a e] (.printStackTrace e)))))