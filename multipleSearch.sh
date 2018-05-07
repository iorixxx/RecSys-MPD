#!/usr/bin/env bash

cd /mnt/recSys/runnable
mkdir -p results

JAR=/mnt/recSys/runnable/mpd.jar
INDEX=/mnt/recSys/runnable/MPD.index
CHALLENGE=/mnt/recSys/challenge_test_set.json
RESULT=/mnt/recSys/runnable/results

SIMILARITIES=( BM25 DPH IB DFI PL2 TFIDF LogTF RawTF )
FILLERS=( Follower Playlist Blended Hybrid )
BOOLEANS=( true false )
MODES=( Mode1 Mode2 Mode3 )

for similarity in "${SIMILARITIES[@]}"
do
	for filler in "${FILLERS[@]}"
	do		
		for bool in "${BOOLEANS[@]}"
		do
			for mode in "${MODES[@]}"
			do
				csv=$RESULT"/"$similarity"-"$filler"-"$bool"-"$mode".csv"
				echo $csv
				
				java -server -Xms10g -Xmx25g -cp $JAR edu.anadolu.app.SearchApp $INDEX $CHALLENGE $csv RECSYS $similarity $filler $bool $mode
				python verify_submission.py $CHALLENGE $csv
			done 
		done 
	done
done



#!/usr/bin/env bash
#cd /mnt/recSys/RecSys-MPD
#git pull
#mvn clean package
#cd /mnt/recSys/
#java -server -Xms10g -Xmx25g -cp /mnt/recSys/RecSys-MPD/target/mpd.jar edu.anadolu.app.SearchApp /mnt/recSys/MPD.index /mnt/recSys/challenge_set.json dph.csv RECSYS DPH Blended false Mode3
#python verify_submission.py challenge_set.json submission.csv