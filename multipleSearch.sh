#!/usr/bin/env bash

cd /mnt/recSys/runnable
mkdir -p results

JAR=/mnt/recSys/runnable/mpd.jar
INDEX=/mnt/recSys/MPD.index/
CHALLENGE=/mnt/recSys/challenge_test_set.json
RESULT=/mnt/recSys/runnable/results

cp /mnt/recSys/RecSys-MPD/target/mpd.jar $JAR

SIMILARITIES=( BM25 DPH TFIDF IB DFI PL2 )
BOOLEANS=( true false )
MODES=( Mode3 )

for similarity in "${SIMILARITIES[@]}"
do
	for bool in "${BOOLEANS[@]}"
	do
		for mode in "${MODES[@]}"
		do
			csv=$RESULT"/"$similarity"-"$bool"-"$mode".csv"
			echo $csv

			java -server -Xms25g -Xmx50g -cp $JAR edu.anadolu.app.SearchApp $INDEX $CHALLENGE $csv RECSYS $similarity $bool $mode true
			java -server -Xms25g -Xmx50g -cp $JAR edu.anadolu.app.SearchApp $INDEX $CHALLENGE $csv RECSYS $similarity $bool $mode false
			java -server -Xms10g -Xmx25g -cp $JAR edu.anadolu.app.FillerApp $INDEX $CHALLENGE $csv
		done
	done
done