#!/usr/bin/env bash

ID=topt
EXP="/apc/experiments/"$ID

META="/apc/metadata"
SRC="/apc/RecSys-MPD"
TEST="/apc/dataset/test/ttv100K"
INDEX="/apc/MPD.index"

JXMS="-Xms80g"
JXMX="-Xmx100g"

SIMILARITY="PL2"
SORTER="GeoSort"
SEARCHFIELD="Track"

TOPK=200
SIZES=(100 200 300 400 500 600 700 800 900 1000)

FEATURES=(1 2 3 4 5 6 7 8 9 10 11 12 13 14 15 16 17 18 19 20 21 22 23 24 25 26 27 28 29 30 31 32 33 34 35 36 37 38)

LTRLIB="jforests"

CUTOFF=100


# go to source folder, pull changes from the repository, and build project
cd $SRC
git pull
mvn clean package


# create and go to sampling folder
mkdir $EXP
cd $EXP

# experiment
for i in "${SIZES[@]}"
do
	mkdir $i
	cd $i
	
	# generate sampling data
	java -server $JXMS $JXMX -cp $SRC"/target/mpd.jar" edu.anadolu.app.BestSearchApp $INDEX $TEST"/train.json" "train.csv" $SIMILARITY $TOPK $i $SORTER $SEARCHFIELD
	java -server $JXMS $JXMX -cp $SRC"/target/mpd.jar" edu.anadolu.app.BestSearchApp $INDEX $TEST"/validation.json" "validation.csv" $SIMILARITY $TOPK $i $SORTER $SEARCHFIELD
	java -server $JXMS $JXMX -cp $SRC"/target/mpd.jar" edu.anadolu.app.BestSearchApp $INDEX $TEST"/test.json" "test.csv" $SIMILARITY $TOPK $i $SORTER $SEARCHFIELD

	# extract learning-to-rank features
	flist=$(printf " %d" "${FEATURES[@]}")
	flist=${flist:1}

	python3 $SRC"/python/submission_to_letor.py" $TEST"/train.json" "train.csv" "train.txt" $META"/"track_metadata.csv $META"/"album_metadata.csv $META"/"artist_metadata.csv $META"/"audio_metadata.csv --features $flist
	python3 $SRC"/python/submission_to_letor.py" $TEST"/validation.json" "validation.csv" "validation.txt" $META"/"track_metadata.csv $META"/"album_metadata.csv $META"/"artist_metadata.csv $META"/"audio_metadata.csv --features $flist
	python3 $SRC"/python/submission_to_letor.py" $TEST"/test.json" "test.csv" "test.txt" $META"/"track_metadata.csv $META"/"album_metadata.csv $META"/"artist_metadata.csv $META"/"audio_metadata.csv --features $flist

	# apply LambdaMART: train, build model, and predict ranking score
	java $JXMS $JXMX -jar $SRC"/jforests/jforests.jar" --cmd=generate-bin --ranking --folder . --file "train.txt" --file "validation.txt" --file "test.txt"
	java $JXMS $JXMX -jar $SRC"/jforests/jforests.jar" --cmd=train --ranking --config-file $RANKING --train-file "train.bin" --validation-file "validation.bin" --output-model "ensemble.txt"
	java $JXMS $JXMX -jar $SRC"/jforests/jforests.jar" --cmd=predict --ranking --model-file "ensemble.txt" --tree-type RegressionTree --test-file "test.bin" --output-file "predictions.txt"

	# generate re-ranked recommendations
	python3 $SRC"/python/submission_rank.py" "ranked.csv" "test.csv" "predictions.txt" $LTRLIB

	# evaluate
	python3 $SRC"/python/evaluate.py" $TEST"/test.json" $CUTOFF --recommendations $original $ranked
done