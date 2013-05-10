(ns spacegame.base-states
  (:use spacegame.spacetime))



(defn linear-value [timestamp value differential]
  (create-linear-timed-state
   timestamp
   {:value value :lin-diff differential}
   (fn [v t] (assoc v :value (-> v :value (+ (* t (v :diff))))))))

(defn value-map [timestamp values]
  (create-timed-state timestamp values
                      (fn [v t0 t1]
                        (into {} (for [[k,v] v] [k, (rewind v t1)]))
                        )))