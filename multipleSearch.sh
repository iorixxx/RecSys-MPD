#!/usr/bin/env bash

cd /mnt/recSys/runnable
mkdir -p results

JAR=/mnt/recSys/runnable/mpd.jar
INDEX=/mnt/recSys/runnable/MPD.index
CHALLENGE=/mnt/recSys/challenge_test_set.json
RESULT=/mnt/recSys/runnable/results

cp /mnt/recSys/RecSys-MPD/target/mpd.jar $JAR
rm -rf $INDEX
cp -r /mnt/recSys/MPD.index/ $INDEX

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

			java -server -Xms25g -Xmx50g -cp $JAR edu.anadolu.app.SearchApp $INDEX $CHALLENGE $csv RECSYS $similarity $bool $mode
			java -server -Xms10g -Xmx25g -cp $JAR edu.anadolu.app.FillerApp $INDEX $CHALLENGE $csv
		done
	done
done