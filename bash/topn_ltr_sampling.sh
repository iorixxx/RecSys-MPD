#!/usr/bin/env bash

META="/apc/metadata"
SRC="/apc/RecSys-MPD"
TEST="/apc/dataset/test/10fold_10K_b"
INDEX="/apc/MPD.index"
SAMPLE="/apc/sample/001"

JXMS="-Xms80g"
JXMX="-Xmx120g"

SIMILARITY="PL2"
SORTER="GeoSort"
SEARCHFIELD="Track"

TOPK=200
SIZES=(50 100 150 200 250)

FEATURES=(1 2 3 4 5 6 7 8 9 10 11 12 13 14 15 16 17 18 19 20 21 22 23 24 25 26 27 28 29 30 31 32 33 34)


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
			
		java -server $JXMS $JXMX -cp $SRC"/target/mpd.jar" edu.anadolu.app.BestSearchApp $INDEX $TEST"/"$playlist $result $SIMILARITY $TOPK $k $SORTER $SEARCHFIELD
	done
done


# extract learning-to-rank features
for k in "${SIZES[@]}"
do
	EXP=$SAMPLE"/"$k
	cd $EXP

	for i in {1..10}
	do
		playlist=$(printf "fold-%03d.json" $i)
		result=$(printf "results-%d.csv" $i)
		letor=$(printf "letor-%d.txt" $i)

		flist=$(printf " %d" "${FEATURES[@]}")
		flist=${flist:1}

		python3 $SRC"/python/submission_to_letor.py" $TEST"/"$playlist $result $letor $META"/"track_metadata.csv $META"/"album_metadata.csv $META"/"artist_metadata.csv $META"/"audio_metadata.csv --features $flist
	done
done


# merge feature partitions into separate training files
for k in "${SIZES[@]}"
do
	EXP=$SAMPLE"/"$k
	cd $EXP
	
	for i in {1..10}
	do
		exc=$(( $i % 10 + 1 ))

		array=(1 2 3 4 5 6 7 8 9 10)

		new_array=()

		for value in "${array[@]}"
		do
			[[ $value != $i && $value != $exc ]] && new_array+=($value)
		done

		array=("${new_array[@]}")
		unset new_array

		args=$(printf " letor-%d.txt" "${array[@]}")
		args=${args:1}

		output=$(printf "train-%d.txt" $i)

		python3 $SRC"/python/merge_letor_files.py" $output --letors $args
	done
done
