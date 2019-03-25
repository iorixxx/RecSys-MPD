#!/usr/bin/env bash

ID=001
FEATURE="/apc/feature/"$ID
SAMPLE="/apc/sample/"$ID

META="/apc/metadata"
SRC="/apc/RecSys-MPD"
TEST="/apc/dataset/test/ttv100K"
INDEX="/apc/MPD.index"


FEATURES=(1 2 3 4 5 6 7 8 9 10 11 12 13 14 15 16 17 18 19 20 21 22 23 24 25 26 27 28 29 30 31 32 33 34 35 36 37 38)


# go to source folder and pull changes from the repository
cd $SRC
git pull


# create and go to feature folder
mkdir $FEATURE
cd $FEATURE


# extract learning-to-rank features
flist=$(printf " %d" "${FEATURES[@]}")
flist=${flist:1}

python3 $SRC"/python/submission_to_letor.py" $TEST"/train.json" $SAMPLE"/train.csv" "train.txt" $META"/"track_metadata.csv $META"/"album_metadata.csv $META"/"artist_metadata.csv $META"/"audio_metadata.csv --features $flist
python3 $SRC"/python/submission_to_letor.py" $TEST"/validation.json" $SAMPLE"/validation.csv" "validation.txt" $META"/"track_metadata.csv $META"/"album_metadata.csv $META"/"artist_metadata.csv $META"/"audio_metadata.csv --features $flist
python3 $SRC"/python/submission_to_letor.py" $TEST"/test.json" $SAMPLE"/test.csv" "test.txt" $META"/"track_metadata.csv $META"/"album_metadata.csv $META"/"artist_metadata.csv $META"/"audio_metadata.csv --features $flist