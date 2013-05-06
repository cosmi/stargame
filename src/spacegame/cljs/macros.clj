(ns spacegame.cljs.macros
  (:use [jayq.macros]))

(defmacro $-> [sel & args]
 `(-> (~'$ ~sel) ~@args))

(defmacro $doto [sel & args]
 `(doto (~'$ ~sel) ~@args))



;; (defmacro let-remote [steps & body]
;;   (assert (even? (count steps)) "Uneven number of elements in let-form")
;;   (assert (= 2 (count steps)) "More than one step not implemented")
;;   (let [steps (partition-all 2 steps)
;;         steps (apply concat
;;                      (for [[a b :as c] steps]
;;                        (if (symbol? a)
;;                          (do [a (list* 'chathorse.cljs.utils/remote b)])
;;                          c)))]
;;     `(do-> chathorse.cljs.utils/remote-m ~steps ~@body)))


(defmacro let-remote* [[sym remotecall] & body]
  `(let [defer# ~remotecall]
     (-> defer# (jayq.core/done (fn [sym#]
                                  (let [~sym (cljs.reader/read-string sym#)] 
                                    ~@body)))))) 


(defmacro let-remote [[sym [uri & args] :as steps] & body]
  (assert (even? (count steps)) "Uneven number of elements in let-form")
  (assert (= 2 (count steps)) "More than one step not implemented")
  `(let-remote* [~sym (spacegame.cljs.utils/remote '~uri ~@args)] ~@body)) 





(defmacro LOG [& msgs]
  `(js/console.log ~@(map (fn [x] `(~'clj->js ~x)) msgs)))

(defmacro on-click
  ([$ sel fun]
     `(spacegame.cljs.main/click ~$ ~sel (fn [] ~fun)))
  ([$ fun]
     `(spacegame.cljs.main/click ~$ (fn [] ~fun))))
