(ns spacegame.test.spacetime
  (:use spacegame.spacetime)
  (:use clojure.test))


(deftest timed-objects
  (let [obj1 (create-timed-object 0 0 (fn [v t0 t1] (+ v (- t1 t0))))]
    (is (= 0 (get-current-value obj1)))
    (is (= 0 (get-current-time obj1)))
    (try
      (dosync
       (rewind! obj1 100)
       (is (= 100 (get-current-value obj1)))
       (is (= 100 (get-current-time obj1)))
       (rewind! obj1 100)
       (is (= 100 (get-current-value obj1)))
       (is (= 100 (get-current-time obj1)))
       (rewind! obj1 200)
       (is (= 200 (get-current-value obj1)))
       (is (= 200 (get-current-time obj1)))
       (throw (Exception. "")))
      (catch Exception e nil))
    (is (= 0 (get-current-value obj1)))
    (is (= 0 (get-current-time obj1)))
    (rewind! obj1 100)
    (is (= 100 (get-current-value obj1)))
    (is (= 100 (get-current-time obj1)))))

(deftest timed-events
  (let [obj1 (create-timed-object 0 0 (fn [v t0 t1] (+ v (- t1 t0))))
        obj2 (create-timed-object -10 10 (fn [v t0 t1] (- v (- t1 t0))))
        event (create-timed-event 100 [obj1 obj2]
                                  (fn[]
                                    (alter-value! obj1 #(+ (get-current-value obj2) %))))]
    (is (= 0 (get-current-value obj1)))
    (is (= 0 (get-current-time obj1)))
    (is (= -10 (get-current-value obj2)))
    (is (= 10 (get-current-time obj2)))
    (rewind! obj1 100)
    (is (= 100 (get-current-value obj1)))
    (is (= 100 (get-current-time obj1)))
    (is (= -10 (get-current-value obj2)))
    (is (= 10 (get-current-time obj2)))
    (rewind! obj1 200)
    (is (= 100 (get-current-value obj1)))
    (is (= 200 (get-current-time obj1)))
    (is (= -100 (get-current-value obj2)))
    (is (= 100 (get-current-time obj2)))))