(ns chathorse.test.handler
  (:use clojure.test
        ring.mock.request
        clojure.pprint
        chathorse.handler
        org.httpkit.server
        org.httpkit.timer
        chathorse.utils
        [ring.util.codec :only [url-encode]])
  (:require [clj-http.client :as http]
            [org.httpkit.client :as client]
            [clj-http.util :as u]
            [clojure.data.json :as json]))

(def ^:dynamic *port*)
(defn set-clj-server [f]
  (with-redefs [;chathorse.handler/FIND-ROOM-TIMEOUT 1000
                chathorse.handler/LISTENER-TIMEOUT 1500
                chathorse.handler/MIN-WAITING-RETRY-TIMEOUT 50
                chathorse.handler/MAX-WAITING-RETRY-TIMEOUT 500
                chathorse.handler/CLOSE-START-TIMEOUT 500
                chathorse.handler/user-db (ref {})
                chathorse.handler/waiting-users (init-waiting-users)]
    (binding [*port* (+ 4300 (rand-int 100))]
      (let [server (run-server
                    app {:port *port*})]
        (with-timeout server 25000 (server)
          (try (f) (finally (server))))))))

(use-fixtures :each set-clj-server)

(def http-request
  (-> clj-http.core/request
      http/wrap-request-timing
      http/wrap-lower-case-headers
      http/wrap-query-params
      http/wrap-basic-auth
      http/wrap-oauth
      http/wrap-user-info
      http/wrap-url
      http/wrap-redirects
      http/wrap-decompression
      http/wrap-input-coercion
      ;; put this before output-coercion, so additional charset
      ;; headers can be used if desired
      http/wrap-additional-header-parsing
      http/wrap-output-coercion
      ;http/wrap-exceptions
      http/wrap-accept
      http/wrap-accept-encoding
      http/wrap-content-type
      http/wrap-form-params
      http/wrap-nested-params
      http/wrap-method
      ;http/wrap-cookies
      ;wrap-links
      http/wrap-unknown-host
      )
  )

(defn post
  [url & [req]]
  (http/check-url! url)
  (http-request (merge req {:method :post :url url})))


(defn remotecall [uid last-msg-id msgs]
  (let [result (post (str "http://localhost:" *port* "/sync?uid=" uid "&last-msg=" last-msg-id)
             {:headers {"content-type"
                        "application/json"}
              :body (json/write-str msgs)})]
    (cond-> result
      (-> result (get :status) (= 200))
      (update-in [:body] net->clj))))
  

(defmacro with-max-time [value & body]
  {:pre [(integer? value)]}
  `(let [start# (. System (nanoTime))
         ret# (do ~@body)]
     (is (>= ~value (/ (double (- (. System (nanoTime)) start#)) 1000000.0)))
     ret#))


(defmacro with-min-time [value & body]
  {:pre [(integer? value)]}
  `(let [start# (. System (nanoTime))
         ret# (do ~@body)]
     (is (<= ~value (/ (double (- (. System (nanoTime)) start#)) 1000000.0)))
     ret#))

(defn pmap [f & colls]
  (doall (map deref (doall (apply map #(future (apply f %&)) colls)))))

(deftest test-app
  (testing "Find target and send message"
    ;; (let [result (remotecall "user1" -1 [])]
    ;;   (is (= (result :status) 500)))
    (let [result (remotecall "user1" -1 [{:type :boot :lang "pl"}])]
      (is (= (result :status) 200))
      (is (= (result :body) [])))
    (let [result (remotecall "user1" -1 [])]
      (is (= (result :status) 200)))
    ;; (let [result (remotecall "user2" -1 [])]
    ;;   (is (= (result :status) 500)))
    (let [result (remotecall "user2" -1 [{:type :boot :lang "pl"}])]
      (is (= (result :status) 200)))
    (with-min-time 100
      (let [result (remotecall "user1" -1 [{:type :start}])]
        (is (= (result :status) 200))
        (is (= (result :body) []))))
    (let [result (remotecall "user2" -1 [{:type :start}])]
      (is (= (result :status) 200))
      (is (= (->> result :body (map #(dissoc % :ts))) [{ :id 0, :type "new", :from "user1"}])))
    (with-max-time 100
      (let [result (remotecall "user1" -1 [])]
        (is (= (result :status) 200))
        (is (= (->> result :body (map #(dissoc % :ts))) [{ :id 0, :type "new", :from "user2"}]))))
    (let [[result3 result4] (pmap #(remotecall % -1 [{:type :boot :lang "pl"}{:type :start}]) ["user3" "user4"])]
      (is (= (result3 :status) (result4 :status) 200))
      (is (= (->> result3 :body (map #(dissoc % :ts))) [{:id 0, :type "new", :from "user4"}]))
      (is (= (->> result4 :body (map #(dissoc % :ts))) [{:id 0, :type "new", :from "user3"}])))
      (let [result
            (with-max-time 100
              (remotecall "user1" 0 [{:type :msg :to "user3" :content "abc" :local-id 1}]))]
        (is (= (result :status) 200))
        (is (= (->> result :body (map #(dissoc % :ts))) [{:id 1, :type "error-msg", :from "user3", :to "user1", :content "Użytkownik rozłączony"}])))
    (let [result
          (with-max-time 100
            (remotecall "user1" 1 [{:type :msg :to "user2" :content "abc" :local-id 2}]))]
      (is (= (result :status) 200))
      (is (= (->> result :body (map #(dissoc % :ts))) [{:id 2, :type "msg", :to "user2", :from "user1",
                                                        :content "abc" :local-id 2}]))

      (is (= (result :body)
             (-> (remotecall "user1" 0 []) :body))  "Old messages are not removed")
      )
  ))




(deftest test-langs
  (testing "Assure correct lang pairing"
    (let [[result1 result2 result3 result4]
          (doall (pmap #(with-max-time 1000
                   (let [ret (remotecall % -1 [{:type :boot :lang %2}{:type :start}])]
                     ret
                     ))
                ["user1" "user2" "user3" "user4"]
                ["pl" "en" "pl" "en"]))]
      (is (= [200 200 200 200] (map :status [result1 result2 result3 result4])))
      (is (= (->> result3 :body (map #(dissoc % :ts))) [{:id 0, :type "new", :from "user1"}]))
      (is (= (->> result4 :body (map #(dissoc % :ts))) [{:id 0, :type "new", :from "user2"}])))

    ))






(deftest test-topics
  (testing "Assure topics request"
     (let [[result1 result2]
          (pmap #(with-max-time 1100
                   (remotecall % -1 [{:type :boot :lang "pl"}{:type :start}])) ["user1" "user2"])]
      (is (apply = 200 (map :status [result1 result2])))
      (is (= (->> result1 :body (map #(dissoc % :ts))) [{:id 0, :type "new", :from "user2"}]))
      (is (= (->> result2 :body (map #(dissoc % :ts))) [{:id 0, :type "new", :from "user1"}])))


     (with-redefs [chathorse.topics/topics (atom {"pl" ["Kanarek"],
                                                  "en" ["Canary"]})]
       (let [result1
             (with-max-time 100
               (remotecall "user1" 0 [{:type :topic-req :to "user2" :local-id 1}]))
             result2
             (with-max-time 100
               (remotecall "user2" 0 []))]
         (is (= (result1 :status) (result2 :status) 200))
         (is (= (->> result1 :body (map #(dissoc % :ts))) [{:id 1, :type "topic", :from "user1" :to "user2"
                                                            :local-id 1 :content "Kanarek"}]))
         (is (= (->> result2 :body (map #(dissoc % :ts))) [{:id 1, :type "topic", :from "user1" :to "user2"
                                                            :content "Kanarek"}]))))

    ))


(deftest test-quit
  (testing "Assure correct quitting"
    (let [[result1 result2]
          (pmap #(with-max-time 1100
                   (remotecall % -1 [{:type :boot :lang "pl"}{:type :start}])) ["user1" "user2"])]
      (is (apply = 200 (map :status [result1 result2])))
      (is (= (->> result1 :body (map #(dissoc % :ts))) [{:id 0, :type "new", :from "user2"}]))
      (is (= (->> result2 :body (map #(dissoc % :ts))) [{:id 0, :type "new", :from "user1"}])))

    (let [result1
          (with-max-time 100
            (remotecall "user1" 0 [{:type :msg :content "abc" :to "user2" :local-id 1}
                                   {:type :quit :to "user2"}
                                   {:type :msg :content "def" :to "user2" :local-id 2}]))
          result2
          (with-max-time 100
            (remotecall "user2" 0 [{:type :msg :content "def" :to "user1" :local-id 1}
                                   {:type :quit :to "user1"}]))
          result3
          (with-max-time 100
            (remotecall "user1" 3 [{:type :msg :content "ghi" :to "user2" :local-id 2}]))]
      (is (apply = 200 (map :status [result1 result2])))
      (is (= (->> result1 :body (map #(dissoc % :ts))) [{:id 1, :type "msg", :from "user1", :to "user2", :local-id 1, :content "abc"}
                                                        {:id 2, :type "quit", :from "user1", :to "user2"}
                                                        {:id 3, :type "error-msg", :from "user2", :to "user1",
                                                         :content "Użytkownik rozłączony"}
                                                        ;; {:id 3, :type "msg", :from "user1", :to "user2", :local-id 2, :content "def"}
                                                        ]))
      (is (= (->> result2 :body (map #(dissoc % :ts))) [{:id 1, :type "msg", :from "user1", :to "user2", :content "abc"}
                                                        {:id 2, :type "quit", :from "user1", :to "user2"}
                                                        {:id 3, :type "error-msg", :from "user1", :to "user2", :content "Użytkownik rozłączony"}
                                                        ;; {:id 3, :type "m, :from "user1", :to "user2" :content "def"}
                                                        ;; {:id 4, :type "error-msg", :from "user1", :to "user2",
                                                        ;;  :content "Użytkownik rozłączony"}
                                                        {:id 4, :type "quit", :from "user2", :to "user1"}]))
      (is (= (->> result3 :body (map #(dissoc % :ts))) [{:id 4, :type "quit", :from "user2", :to "user1"}
                                                        {:id 5, :type "error-msg", :from "user2", :to "user1",
                                                         :content "Użytkownik rozłączony"}]))
      )))



(deftest test-many-users
  (testing "Assure correct pairing with a large amount of users"
    (let [usernames (map #(str "user" %) (range 1 101))
          results (doall (pmap #(with-max-time 1200
                                  (remotecall % -1 [{:type :boot :lang "pl"}{:type :start}])) usernames))]
      
      (is (every? #(= % 200) (map :status results)))
      (is (every? #(-> % count (= 1)) (map :body results)))
      (is (= (set usernames) (set (map #(-> % :body first :from) results))))

      (let [conns (zipmap usernames (map #(-> % :body first :from) results))]
        (is (every? #(-> conns (get (val %)) (= (key %))) conns))
        (let [results2 (doall (pmap #(with-max-time 1100
                                  (remotecall % 0 [{:type :start}])) usernames))]
          (is (every? #(= % 200) (map :status results)))
          (is (every? #(-> % count (= 1)) (map :body results)))
          (is (= (set usernames) (set (map #(-> % :body first :from) results))))
          (let [conns2 (zipmap usernames (map #(-> % :body first :from) results2))]
            (is (every? #(-> conns2 (get (val %)) (= (key %))) conns2))
            (is (every? true? (map not= (map conns usernames) (map conns2 usernames))))))))))

(deftest test-prefs
  (testing "Assure correct pairing with preferences"
    (let [amount 4
          ages-me (mapcat #(repeat amount %) (range 1 (+ 1 amount)))
          ages-them (apply concat (repeat amount (range 1 (+ 1 amount))))
          ages (remove nil? (map #(when (not= %1 %2) [%1 %2]) ages-me ages-them))
          ages-me (map first ages)
          ages-them (map second ages)
          usernames (map #(str "user" %1 %2) ages-me ages-them)
          expected (into {} (map #(vector (str "user" %1 %2) (str "user" %2 %1)) ages-me ages-them))
          results (doall
                   (map #(future
                           (with-max-time 11000
                            (loop [res (remotecall %1 -1 [{:type :boot :lang "pl"}
                                                          {:type :start :prefs {:me {:age (* 10 %2)} :them {:age (* 10 %3)}}}])]
                              (if (-> res :body not-empty)
                                res
                                (recur (remotecall %1 -1 []))))))
                         usernames ages-me ages-them))
          results (map deref results)]

      (is (every? #(= % 200) (map :status results)))
      (is (every? #(-> % count (= 1)) (map :body results)))
      (is (= (set usernames) (set (map #(-> % :body first :from) results))))
      (let [conns (zipmap usernames (map #(-> % :body first :from) results))]
        (is (= conns expected)))
      )))
      

(deftest test-avatar-and-card
  (testing "Assure topics request"
     (let [[result1 result2]
          (pmap #(with-max-time 1100
                   (remotecall % -1 [{:type :boot :lang "pl"}{:type :start}])) ["user1" "user2"])]
      (is (apply = 200 (map :status [result1 result2])))
      (is (= (->> result1 :body (map #(dissoc % :ts))) [{:id 0, :type "new", :from "user2"}]))
      (is (= (->> result2 :body (map #(dissoc % :ts))) [{:id 0, :type "new", :from "user1"}]))
      (let [result
            (with-max-time 1100
              (remotecall "user1" 0 [{:type :msg :content "abc" :avatar "m" :to "user2" :local-id 1}]))]
        (is (= 200 (result :status)))
        (is (= (->> result :body (map #(dissoc % :ts))) [{:id 1, :type "msg" :content "abc" :avatar "m" :to "user2" :from "user1"
                                                          :local-id 1}])))
      (let [result
            (with-max-time 1100
              (remotecall "user2" 0 []))]
        (is (= 200 (result :status)))
        (is (= (->> result :body (map #(dissoc % :ts)))
               [{:id 1, :type  "msg" :content "abc" :avatar "m" :to "user2" :from "user1"}])))
      (let [result
            (with-max-time 1100
              (remotecall "user1" 1 [{:type :msg :content "abc" :avatar "m" :to "user2" :local-id 1 :special "card"}]))]
        (is (= 200 (result :status)))
        (is (= (->> result :body (map #(dissoc % :ts))) [{:id 2, :type  "msg" :content "abc" :avatar "m" :to "user2" :from "user1"
                                                          :local-id 1 :special "card"}])))
      (let [result
            (with-max-time 1100
              (remotecall "user2" 1 []))]
        (is (= 200 (result :status)))
        (is (= (->> result :body (map #(dissoc % :ts)))
               [{:id 2, :type  "msg" :content "abc" :avatar "m" :to "user2" :from "user1" :special "card"}])))
      )))

(deftest test-censor
  (testing "Assure topics request"
     (let [[result1 result2]
          (pmap #(with-max-time 1100
                   (remotecall % -1 [{:type :boot :lang "pl"}{:type :start}])) ["user1" "user2"])]
      (is (apply = 200 (map :status [result1 result2])))
      (is (= (->> result1 :body (map #(dissoc % :ts))) [{:id 0, :type "new", :from "user2"}]))
      (is (= (->> result2 :body (map #(dissoc % :ts))) [{:id 0, :type "new", :from "user1"}]))

      (let [result
            (with-max-time 1100
              (remotecall "user1" 0 [{:type :msg :content "kurwa chujowo" :to "user2" :local-id 1}]))]
        (is (= 200 (result :status)))
        (is (= (->> result :body (map #(dissoc % :ts)))
               [{:id 1, :type "msg" :content "****a ****owo" :to "user2" :from "user1"
                 :local-id 1}])))
      
      (let [result
            (with-max-time 1100
              (remotecall "user2" 0 []))]
        (is (= 200 (result :status)))
        (is (= (->> result :body (map #(dissoc % :ts)))
               [{:id 1, :type  "msg" :content "****a ****owo" :to "user2" :from "user1"}]))))))



(deftest test-boot
  (testing "Find target and send message"
    (let [result (with-min-time 100
                   (remotecall "user1" -1 [{:type :boot :lang "pl"} {:type :start} ]))]
      (is (= (result :status) 200))
      (is (= (result :body) [])))
    (let [result (with-max-time 1000
                   (remotecall "user2" -1 [{:type :boot :lang "pl"} {:type :start} ]))]
      (is (= (result :status) 200))
      (is (= (->> result :body (map #(dissoc % :ts))) [{:id 0 :type "new" :from "user1"}])))
    (let [result (with-min-time 1000
                   (remotecall "user1" -1 [{:type :boot :lang "pl"} {:type :start} ]))]
      (is (= (result :status) 200))
      (is (= (result :body) [])))
    (let [result (with-max-time 100
                   (remotecall "user2" 0 []))]
      (is (= (result :status) 200))
      (is (= (->> result :body (map #(dissoc % :ts))) [{:id 1, :type "quit", :from "user1", :to "user2"}])))
    (let [result (with-min-time 1000
                   (remotecall "user2" 1 [{:type :start} ]))]
      (is (= (result :status) 200))
      (is (= (->> result :body (map #(dissoc % :ts))) [])))
    ))
