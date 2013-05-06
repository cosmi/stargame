(ns spacegame.config
  (:use [clojure.java.io :only [file]]
        [clj-config.core :only [safely read-config]]))

(defn find-config-file-on-classpath [fname]
  (-> (Thread/currentThread)
      .getContextClassLoader 
      (.findResource fname)))

(defn find-config-file [fname]
  (let [cfile (file (System/getProperty "user.dir") "config" fname)]
    (if (and cfile (.exists cfile))
      cfile
      (file (find-config-file-on-classpath fname)))))

(defn- read-config-file [fname]
  (safely read-config (file (find-config-file-on-classpath fname))))

(def config (read-config-file "chathorse.conf.clj"))

(def LANGS (-> config (get :langs) set))