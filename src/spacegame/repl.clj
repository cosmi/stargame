(ns spacegame.repl
  (:use [spacegame.handler :only [app]]
        ;[chathorse.boxes :only [trash-zombies! trash-disconnected-users!]]
        ;[chathorse.mongo :only [init-mongo]]
        ;[dieter.core]
        )
  (:require [org.httpkit.server]))


(defn -main [& args]
  (org.httpkit.server/run-server #(apply
                                   (-> #'app
                                       ) %&) {:port 8667})
  (println "Server started at port 8667.")
  )