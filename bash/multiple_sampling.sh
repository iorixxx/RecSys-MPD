#!/usr/bin/env bash


SRC="/apc/RecSys-MPD"
TEST="/apc/dataset/test/10fold_10K_b"
INDEX="/apc/MPD.index"
SAMPLE="/apc/sample/001"

JXMS="-Xms80g"
JXMX="-Xmx120g"

TWM="PL2"
RMS="GeoSort"
PR="Track"

TOPK=200
SIZES=(50 100 150 200 250)

# go to source folder, pull changes from the repository, and build project
cd $SRC
git pull
mvn clean package


# generate sampling data
for k in "${SIZES[@]}"
do
	EXP=$SAMPLE"/"$k
	
	mkdir $EXP
	cd $EXP

	for i in {1..10}
	do
		playlist=$(printf "fold-%03d.json" $i)
		result=$(printf "results-%d.csv" $i)
			
		java -server $JXMS $JXMX -cp $SRC"/target/mpd.jar" edu.anadolu.app.BestSearchApp $INDEX $TEST"/"$playlist $result $TWM $TOPK $k $RMS $PR		
	done
done

