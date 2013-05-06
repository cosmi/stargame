(ns chathorse.topics
  (:use [chathorse.config :only [config]])
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.set :as set]
            [hiccup.core :as hc]
            [noir.response :as response]))

(def topics (atom {}))

(defn check-lang
  ([lang] (check-lang lang "pl"))
  ([lang default]
     (if ((config :langs) (and lang (name lang)))
       (name lang)
       default)))

(defn topics-file [lang]
  (str (config :topics-dir) lang ".txt"))


(defn load-topics! [lang]
  (let [lang (check-lang lang)
        lines (->> (topics-file lang)
                   io/file
                   io/reader
                   line-seq
                   (map str/trim)
                   (remove empty?))]
    (swap! topics assoc lang (vec lines))))

(defn save-topics! [lang]
  (when-let [lang (check-lang lang nil)]
    (spit (io/file (topics-file lang)) (str/join "\n" (@topics lang)))))




(defn get-topic [lang]
  (let [lang (check-lang lang)]
    (when-let [topics (@topics lang)]
      (rand-nth topics))))



(def forbidden-words (atom {}))
(def forbidden-patterns (atom {}))
(defn censor-file [lang]
  (str (config :censor-dir) lang ".txt"))

(defn make-pattern [input]
  (->> input
       ;(map #(java.util.regex.Pattern/quote %))
       (interpose "|")
       (apply str)
       re-pattern))

(defn load-censor! [lang]
  (let [lang (check-lang lang)
        lines (->> (censor-file lang)
                   io/file
                   io/reader
                   line-seq
                   (map str/trim)
                   (remove empty?))
                   ]
    ;(swap! sorted-topics assoc lang lines)
    (swap! forbidden-words assoc lang (vec lines))
    (swap! forbidden-patterns assoc lang (make-pattern lines))

    ))

(defn save-censor! [lang]
  (when-let [lang (check-lang lang nil)]
    (spit (io/file (censor-file lang)) (str/join "\n" (@forbidden-words lang)))))



(doseq [lang (config :langs)]
  (load-topics! lang)
  (load-censor! lang))

(defn apply-censor [s lang]
  (str/replace s (@forbidden-patterns lang) (fn [input] (let [c (count input)]
                                                        (apply str (repeat c \* ))))))




(defn topics-admin-view [lang]
  (let [lang (check-lang lang)]
   (hc/html
    [:html
     [:body
      [:p "Język: " lang (map #(-> [:a {:style "margin-left:2ex;" :href (str "?lang=" %)} %]) (config :langs))]
      [:form {:method "POST"}
       [:textarea {:name "topics" :cols 80 :rows 20} (str/join "\n" (@;sorted-
                                                                     topics lang))]
       [:input {:type "hidden" :name "lang" :value lang}]
       [:br]
       [:input {:type :submit :value "Zapisz"}]]]])))

(defn topics-admin-change! [lang content]
  (when-let [lang (check-lang lang nil)]
    (let [lines (remove empty? (map str/trim (str/split content #"\r?\n")))]
      ;(swap! sorted-topics assoc lang lines)
      (swap! topics assoc lang (vec lines)))
    (save-topics! lang)
    (response/redirect (str "/admin/topics?lang=" lang))))




(defn censor-admin-view [lang]
  (let [lang (check-lang lang)]
   (hc/html
    [:html
     [:body
      [:p "Język: " lang (map #(-> [:a {:style "margin-left:2ex;" :href (str "?lang=" %)} %]) (config :langs))]
      [:form {:method "POST"}
       [:textarea {:name "words" :cols 80 :rows 20} (str/join "\n" (@forbidden-words lang))]
       [:input {:type "hidden" :name "lang" :value lang}]
       [:br]
       [:input {:type :submit :value "Zapisz"}]]]])))

(defn censor-admin-change! [lang content]
  (when-let [lang (check-lang lang nil)]
    (let [lines (remove empty? (map str/trim (str/split content #"\r?\n")))
          pattern (make-pattern lines)]
      (swap! forbidden-words assoc lang (vec lines))
      (swap! forbidden-patterns assoc lang pattern))
    (save-censor! lang)
    (response/redirect (str "/admin/censor?lang=" lang))))