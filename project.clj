(defproject spacegame "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [compojure "1.1.5"]
                 [com.novemberain/monger "1.4.2"]
                 [cheshire "5.0.2"]
                 [hiccup "1.0.2"]
                 [ring "1.1.8"]
                 [http-kit "2.0.1"]
                 [scijors "1.0.0-SNAPSHOT"]
                 [jayq "2.3.0"]
                 [hiccups "0.2.0"]
                 ;[dieter "0.3.0"]
                 [me.raynes/fs "1.4.0"   :exclusions [org.clojure/clojure]]
                 ;[fs "0.11.1"]
                 [clj-http "0.6.5"]
                 [org.clojure/data.json "0.2.1"]
                 [ring-http-basic-auth "0.0.2"]]
  :dev-dependencies [[lein-swank "1.4.5"]
                     [org.clojure/tools.nrepl "0.2.1"]]
  :plugins [[lein-ring "0.8.2"]
            [lein-swank "1.4.5"]
            [lein-cljsbuild "0.3.0"]
            [lein-pprint "1.1.1"] ]
  :native-path "native"
  :profiles {
             :dev 
             {
              :dependencies [[ring-mock "0.1.3"]]
              :resource-paths ["target-resources" "config/dev"]
              :cljsbuild {
                          :builds [
                                    {:source-paths ["src/spacegame/cljs"]
                                     :notify-command ["./cljs-separate.sh" "main" "target-resources/public/js" "spacegame.cljs.main"]
                                     
                                     ;:jar true
                                     :compiler {:output-to "target-resources/public/js/main.js"
                                                ;:output-dir "target-resources/public/js"
                                                ;:optimizations :whitespace
                                                :pretty-print true
                                                }}]
                          }}}
  :ring {:handler spacegame.handler/app}
  :repositories [["internal" {:url "http://repo.zonar.pl"
                              :username "pantadeusz" :password "kupakupa"}]])
