#!/bin/bash

ns="$1"
dir="$2"
sep="goog.provide(\"$3\");"


fname="$dir/$ns.js"
fname2="$dir/_$ns.js"
fnametmp="$dir/$ns.js.tmp"
echo "sed $fnametmp -ne \"/$sep/,\$p\" > $fname"
echo "sed $fnametmp -e \"/$sep/,\$d\" > $fname2"


mv $fname $fnametmp
sed $fnametmp -ne "/$sep/,\$p" > $fname
sed $fnametmp -e "/$sep/,\$d" > $fname2


