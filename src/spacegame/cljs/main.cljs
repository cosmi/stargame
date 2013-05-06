(ns chathorse.cljs.main
  (:require-macros [hiccups.core :as hiccups])
  (:require [hiccups.runtime :as hiccupsrt]
            [clojure.browser.repl :as repl]
            [goog.dom.forms :as gforms]
            [goog.style :as style]
            [clojure.browser.event :as event]
            [jayq.core :as jq])
  (:use-macros [hiccups.core :only [html]]
               [chathorse.cljs.macros :only [$-> $doto let-remote on-click LOG]]
               [jayq.macros :only [ready queue]])
  (:use [chathorse.cljs.utils :only [remote serialize-clj]]
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

(repl/connect "http://localhost:9000/repl")

 



(defn click  
  ([$ sel fun]
     (on $ "click" sel fun)) 
  ([$ fun] 
     (on $ "click" fun)))

   

(defn btn [classes text ]
  [:button {:class classes} text])
 
(defn render-chatbox [id]
  (html  
   [:div.chatbox.span6 {:id (str "box-" id)
                  :style "display:block"
                  :data-boxid id}

    [:div.top
     [:button.btn.kill [:i.icon-remove]]
     [:button.btn.refresh [:i.icon-refresh]]
     ]  
    [:div.output "Otwarty box " id]
    [:div.bottom
     [:form {:action ""}
      [:input.input]
      [:button.send "Wyślij"]]]]
   ))


(defn render-pref-form []
  (html
   [:span
    [:form.prefform {:action ""}
    [:div.chatbox.span6 {:id (str "box-" id)
                         :style "display:block"
                         :data-boxid id}
     [:div.top
      [:button.btn.kill [:i.icon-remove]]
      ]
     [:div.output
      [:div "Płeć"
       [:label "Kobieta" [:input {:type :radio :name :gender :value :female}]]
       [:label "Mężczyzna" [:input {:type :radio
                                    :name :gender :value :male}]]]
      [:label "Wiek" [:input {:name :age :value (+ 10 (rand-int 15))}]]
      [:label "Miejsce" [:input {:name :loc :value (rand-nth ["Kraków" "Warszawa" "Łódź" "Katowice"])}]]
      [:div "Preferowana płeć"
       [:label "Kobieta" [:input {:type :radio :name :gender? :value :female}]]
       [:label "Mężczyzna" [:input {:type :radio :name :gender? :value :male}]]]
      [:label "Podobny wiek" [:input {:type :checkbox :name :age?}]]
      [:label "Ta sama lokalizacja" [:input {:type :checkbox :name :location?}]]
      [:input {:type :hidden :name :lang :value :pl}]]
     
     [:div.bottom
      [:button.send "Połącz"]]]]]
   )) 

(defn chatbox-map [box]  
  (into {:box (first box)} 
        (for [s [:input :connect :output :send]] 
          [s ($-> box (find (->> s name (str "."))) first)])))

(defn create-new-chatbox [inside id]  
  (let [code ($-> (render-chatbox id))]
    (inner inside code)
    code
    ))




(defn chatbox-append-msgs [user-id box msgs]
  (let [output ($-> box (find :.output))]
    (doseq [msg msgs]
      (case (msg :type)
        :chat (append output (html [:div.chatline
                                    [:span.username
                                     (if (= (msg :user-id) user-id)
                                       "Ty"
                                       "Nieznajomy")] " "
                                    [:span.content
                                     (msg :content)] 
                                    ]))
        
        ))))

(defn refresh-box [box user-id box-id last-msg-id] 
  (let-remote [ new-msgs ("/get-new-msgs" user-id box-id @last-msg-id)]
    (swap! last-msg-id max (apply max (map :id new-msgs)))
    (chatbox-append-msgs user-id box new-msgs)))

(ready
  (doseq [panel ($-> :.user-box)]
    (let-remote [user-id ("/gen-user-id")]
      (let [chatbox-holder ($-> panel (find :.chatbox-holder) first)]
        ($-> panel (find :.user-id) (inner user-id)) 
        ($doto panel
               (on-click :.new-box
                         (let [info ($-> (render-pref-form) (append-to chatbox-holder))]
                           ($doto info
                                  (on "click" :.kill #(do ($-> info jq/remove) (.preventDefault %)))
                                  (on "submit" :form.prefform
                                      (fn[ev]
                                        (.preventDefault ev)
                                        (let-remote [box-id ("/find-box" user-id 0.090 ($-> info serialize-clj))]
                                          (let [box (create-new-chatbox info box-id) 
                                                last-msg-id (atom nil)]
                                            ($doto box
                                                   (on-click :.refresh
                                                             (refresh-box box user-id box-id last-msg-id))
                                                   (on "submit" ".bottom form" 
                                                       #(let [message ($-> box (find :.input) jq/val)]
                                                          ($-> box (find :.input) (jq/val ""))
                                                          (let-remote [msg ("/send-to-box" user-id box-id message)]
                                                            (refresh-box box user-id box-id last-msg-id))
                                                          (.preventDefault %)))
                                                   (on-click :.kill
                                                             (do
                                                               (remote "/leave-box" user-id box-id)
                                                               (jq/remove box)))
                                                   )))))))))))))