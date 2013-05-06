#!/bin/bash

lein2 cljsbuild auto &
CLJS_PID=$!



#if [ "$CLOJURESCRIPT_HOME" = "" ]; then
#  CLOJURESCRIPT_HOME="`dirname $0`/.."
#fi

#CLJSC_CP=''
#for next in lib/: lib/*: src/clj: src-cljs: test/cljs; do
#  CLJSC_CP=$CLJSC_CP$CLOJURESCRIPT_HOME'/'$next
#done
#echo $CLJSC
#java -server -cp $CLJSC_CP clojure.main -e \
#"(require '[cljs.repl :as repl]) (require '[cljs.repl.browser :as browser]) (def env (browser/repl-env)) (repl/repl env)" &
#JAVA_PID=$!


eatmydata lein2 swank-wrap 4005 spacegame.repl
echo dupa $CLJS_PID `pgrep -P $CLJS_PID`
kill `pgrep -P $CLJS_PID`
kill $CLJS_PID
#kill $JAVA_PID

