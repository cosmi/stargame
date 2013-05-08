(ns spacegame.spacetime)


(defprotocol PTimedEvent
  (get-event-time [_])
  (evaluate [_]))


(defprotocol PTimedState
  (rewind [_ to-time])
  (get-current-time [_])
  (get-current-state [_]))


(defprotocol PTimedObject
  ;; requires PTimedState
  (add-event [at-time object]))

(deftype TimedState
    ;; Immutable Class
    [value timestamp progress-function]
  (rewind [_ to-time]
    (assert (<= timestamp to-time))
    (TimedState. (progress-function value timestamp to-time) to-time progress-function) ;Immutable!
    )
  (get-current-time [_] timestamp)
  (get-current-state [_] value))



(deftype TimedObject
    ;; Mutable Class
    [state event-queue]
  PTimedState
  (rewind [this to-time]
    (let [old-time (get-current-time this)]
      (assert (<= old-time to-time))
      (loop []
        (let [breakpoint (-> event-queue ensure first)
              time (key breakpoint)]
          (assert (>= time old-time))
          (when (-> time (< to-time))
              (when (not-empty (-> breakpoint val))
                (alter state rewind time))
              (alter event-queue dissoc time) ;; najpierw kasujemy listę
              (doseq [event (-> breakpoint val reverse)] ;; reverse => w kolejności dodawania
                (evaluate event))
              (recur))))))
  (get-current-time [_] (get-current-time state))
  (get-current-state [_] (get-current-state state))
  PTimedObject
  (add-event [this event]
    (let [at-time (get-event-time event)]
      (assert (>= at-time (get-current-time this)))
      (alter event-queue update-in [at-time] conj event))))



(defn create-timed-object [value initial-time progress-function]
  (assert (integer? value))
  (TimedObject. (ref (TimedState. value initial-time progress-function))
                ;{initial-time nil} daje gwarancję, że mapa będzie zawierać tylko integery (tak działa sorted-map)
                (ref (sorted-map initial-time nil))))


(deftype TimedEvent [timestamp fun evaluated?]
    PTimedEvent
  (get-event-time [_] timestamp)
  (evaluate [_]
    (when-not (ensure evaluated?)
      (fun))))

(defn create-timed-event [timestamp objects fun]
  (TimedEvent. timestamp
               #(do
                  (doseq [obj objects]
                    (rewind obj timestamp))
                  (fun)
                  (doseq [obj objects]
                    (assert (= timestamp (get-current-time obj)))))
               (ref false)))

