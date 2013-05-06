(ns spacegame.repl
  (:use [spacegame.handler :only [app]]
        ;[chathorse.boxes :only [trash-zombies! trash-disconnected-users!]]
        ;[chathorse.mongo :only [init-mongo]]
        ;[dieter.core]
        )
  (:require [org.httpkit.server]))

(defn dieter-options
  ([mode]
     {
      :engine     :v8 ; defaults to :rhino; :v8 is much much faster
      :compress   (not (= mode :dev))     ; minify using Google Closure Compiler
      :asset-roots ["resources"]          ; must have a folder called 'assets'. Searched for assets in the order listed.
      :cache-root "resources/asset-cache" ; compiled assets are cached here
      :cache-mode (if (= mode :dev) :development :production)           ; or :production. :development disables cacheing
      :log-level  :normal                 ; or :quiet
       ; list of files for `lein dieter-precompile` to precompile. If left blank (the default), all files will be precompiled, and errors will be ignored.
      })
  ([]
     (dieter-options :prod)))



(defn -main [& args]
  (org.httpkit.server/run-server #(apply
                                   (-> #'app

                                       ;(asset-pipeline (dieter-options :dev))

                                       ) %&) {:port 8666})
  ;(init-mongo)
  (println "Server started at port 8666.")

;; TODO: to powinno oczyszczać pamięć!

  )