(ns spacegame.spacetime)


(defprotocol PTimedEvent
  (get-event-time [_])
  (evaluate-event! [_])
  (cancel-event! [_]))


(defprotocol PTimedState
  (rewind [_ to-time])
  (get-time [_])
  (get-value [_])
  (alter-value [_ fun]))


(defprotocol PTimedObject
  ;; requires PTimedState
  (rewind! [this to-time])
  (get-current-time [_] )
  (get-current-value [_] )
  (add-event! [_ event])
  (alter-value! [_ fun]))

(defrecord TimedState
    ;; Immutable Class
    [value timestamp progress-function]
  PTimedState
  (rewind [this to-time]
    (assert (<= timestamp to-time))
    (if (< timestamp to-time)
      (TimedState. (progress-function value timestamp to-time) to-time progress-function)
      this)
                                        ;Immutable!
    )
  (get-time [_] timestamp)
  (get-value [_] value)
  (alter-value [this fun] (assoc this :value (-> this .value fun))))


(defn create-timed-state [timestamp value progress-function]
  (TimedState. value timestamp progress-function))
(defn create-linear-timed-state [timestamp value progress-function]
  (TimedState. value timestamp (fn [v t0 t1]
                                (progress-function v (- t1 t0)))))


(defrecord TimedObject
    ;; Mutable Class
    [state event-queue]
  PTimedObject
  (rewind! [this to-time]
    (dosync
     (let [old-time (get-current-time this)]
       (assert (<= old-time to-time))
       (when (< old-time to-time)
         (loop []
           (when-let [breakpoint (-> event-queue ensure first)]
             (let [time (key breakpoint)]
               (assert (>= time old-time))
               (when (-> time (< to-time))
                 (when (not-empty (-> breakpoint val))
                   (alter state rewind time))
                 (alter event-queue dissoc time) ;; najpierw kasujemy listę
                 (doseq [event (-> breakpoint val)] ;; reverse => w kolejności dodawania
                   (evaluate-event! event))
                 (recur)))))
         (alter state rewind to-time)))))
  (get-current-time [_] (dosync (get-time (ensure state))))
  (get-current-value [_] (dosync (get-value (ensure state))))
  
  (add-event! [this event]
    (dosync
     (let [at-time (get-event-time event)]
       (assert (>= at-time (get-current-time this)))
       (alter event-queue update-in [at-time] conj event))))
  (alter-value! [this fun]
    (dosync (alter state alter-value fun))))



(defn create-timed-object
  ([state]
     (assert (satisfies? PTimedState state))
     (TimedObject. (ref state) (ref (sorted-map (get-time state) nil))))
  ([value initial-time progress-function]
     (assert (integer? initial-time))
     (create-timed-object (TimedState. value initial-time progress-function))))








(defrecord TimedEvent [timestamp fun evaluated?]
  PTimedEvent
  (get-event-time [_] timestamp)
  (evaluate-event! [_]
    (dosync
     (when-not (ensure evaluated?)
       (fun))))
  (cancel-event! [_]
    (dosync
     (when-not (ensure evaluated?)
       (fun))))
  )

(defn create-timed-event [timestamp objects fun]
  (let [event (TimedEvent. timestamp
                           #(do
                              (doseq [obj objects]
                                (rewind! obj timestamp))
                              (fun)
                              (doseq [obj objects]
                                (assert (= timestamp (get-current-time obj)))))
                           (ref false))]
    (doseq [obj objects]
      (add-event! obj event))))


