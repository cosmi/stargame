(ns spacegame.cljs.utils
  (:require ;[redlobster.promise :as promise]
            [hiccups.runtime :as hiccupsrt]
            [clojure.browser.repl :as repl]
            [jayq.core :as jq]
            [goog.dom.forms :as gforms]
            [goog.style :as style]
            [clojure.browser.event :as event])
 
  (:use [jayq.core :only [crate-meta ->selector $ anim text css attr prop remove-attr remove-prop
                          data add-class remove-class toggle-class has-class is after before append
                          prepend append-to prepend-to insert-before insert-after replace-with remove
                          hide show toggle fade-out fade-in slide-up slide-down siblings parent parents
                          parents-until children next prev next-all prev-all next-until prev-until find
                          closest clone inner empty val queue dequeue document-ready
                          clj-content-type? ->content-type preprocess-request ->ajax-settings ajax xhr
                          read bind unbind trigger delegate ->event on one off prevent height width
                          inner-height inner-width outer-height outer-width offset offset-parent position
                          scroll-left scroll-top then done fail progress promise always reject reject-with
                          notify notify-with resolve resolve-with pipe state $deferred $when]]
        [cljs.reader :only [read-string]]) 
  
  (:require-macros [hiccups.core :as hiccups])
  (:use-macros [hiccups.core :only [html]]
               [spacegame.cljs.macros :only [$-> $doto]] 
               [jayq.macros :only [ready queue]])  
  )
 

  
;; (defn remotep [uri & args]
;;   (let [promise (promise/promise)]
;;     (xhr [:POST uri] (prn-str (vec args))  #(p/realise promise (read-string %)))
;;     promise))

(defn serialize-clj [$elem]
  (into {} (->> $elem .serializeArray js->clj (map #(vector (% "name") (% "value"))))))
(defn remote [uri & args]
  (if (= 'apply uri)
    (apply remote (apply list* args))
    (let [params (clj->js (merge {:type "POST"
                                  :data {:args (prn-str args)}}))]
      (.ajax js/jQuery uri params))))


(def remote-m
  {:return identity
   :bind (fn [x f]
           (let [dfd ($deferred)]
             (done x
                   (fn [v]
                     (js/console.log v)
                     (done (f (read-string v))) (partial resolve dfd)))
             (promise dfd)))
   :zero identity})