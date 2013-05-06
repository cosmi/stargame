(ns spacegame.cljs.main
  (:require-macros [hiccups.core :as hiccups])
  (:require [hiccups.runtime :as hiccupsrt]
            [clojure.browser.repl :as repl]
            [goog.dom.forms :as gforms]
            [goog.style :as style]
            [clojure.browser.event :as event]
            [jayq.core :as jq])
  (:use-macros [hiccups.core :only [html]]
               [spacegame.cljs.macros :only [$-> $doto let-remote on-click LOG]]
               [jayq.macros :only [ready queue]])
  (:use [spacegame.cljs.utils :only [remote serialize-clj]]
        [jayq.core :only [crate-meta ->selector $ anim text css attr prop remove-attr remove-prop
                          data add-class remove-class toggle-class has-class is after before append
                          prepend append-to prepend-to insert-before insert-after replace-with 
                          hide show toggle fade-out fade-in slide-up slide-down siblings parent parents
                          parents-until children next prev next-all prev-all next-until prev-until find
                          closest clone inner empty queue dequeue document-ready
                          clj-content-type? ->content-type preprocess-request ->ajax-settings ajax xhr
                          read bind unbind trigger delegate ->event on one off prevent height width
                          inner-height inner-width outer-height outer-width offset offset-parent position
                          scroll-left scroll-top then done fail progress promise always reject reject-with
                          notify notify-with resolve resolve-with pipe state]]
        [cljs.reader :only [read-string]]
         ))

;(repl/connect "http://localhost:9000/repl")

 



(defn click  
  ([$ sel fun]
     (on $ "click" sel fun)) 
  ([$ fun] 
     (on $ "click" fun)))

(defn adder [] 
  (html
   [:form
    [:input {:name a}]
    [:input {:name b}]
    [:div "###"]]
   )
  )


(ready
 (def $content ($ :#main))


 (let [form ($-> (adder) (append-to $content))]
   ($-> form (on "change" :input #(let [vals ($-> form (find :input) (->> (map $) (map jq/val) (map int)))]
                                    (let-remote [res (apply "/plus" vals)]
                                                (js/console.log res)))))
   )
  )