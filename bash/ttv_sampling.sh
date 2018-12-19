
#!/usr/bin/env bash

ID=0
EXP="/apc/experiments/"$ID

META="/apc/metadata"
SRC="/apc/RecSys-MPD"
TEST="/apc/dataset/test/ttv100K"
INDEX="/apc/MPD.index"

JXMS="-Xms40g"
JXMX="-Xmx80g"

SIMILARITY="PL2"
SORTER="GeoSort"
SEARCHFIELD="Track"


TOPK=200
TOPT=500

FEATURES=(1 2 3 4 5 6 7 8 9 10 11 12 13 14 15 16 17 18 19 20 21 22 23 24 25 26 27 28 29 30 31 32 33 34)


# go to source folder, pull changes from the repository, and build project
cd $SRC
git pull
mvn clean package


# go to experiments folder, create a new folder with id
mkdir $EXP
cd $EXP


# generate sampling data
java -server $JXMS $JXMX -cp $SRC"/target/mpd.jar" edu.anadolu.app.BestSearchApp $INDEX $TEST"/train.json" "train.csv" $SIMILARITY $TOPK $TOPT $SORTER $SEARCHFIELD
java -server $JXMS $JXMX -cp $SRC"/target/mpd.jar" edu.anadolu.app.BestSearchApp $INDEX $TEST"/validation.json" "validation.csv" $SIMILARITY $TOPK $TOPT $SORTER $SEARCHFIELD
java -server $JXMS $JXMX -cp $SRC"/target/mpd.jar" edu.anadolu.app.BestSearchApp $INDEX $TEST"/test.json" "test.csv" $SIMILARITY $TOPK $TOPT $SORTER $SEARCHFIELD


# extract learning-to-rank features
flist=$(printf " %d" "${FEATURES[@]}")
flist=${flist:1}

python3 $SRC"/python/submission_to_letor.py" $TEST"/train.json" "train.csv" "train.txt" $META"/"track_metadata.csv $META"/"album_metadata.csv $META"/"artist_metadata.csv $META"/"audio_metadata.csv --features $flist
python3 $SRC"/python/submission_to_letor.py" $TEST"/validation.json" "validation.csv" "validation.txt" $META"/"track_metadata.csv $META"/"album_metadata.csv $META"/"artist_metadata.csv $META"/"audio_metadata.csv --features $flist
python3 $SRC"/python/submission_to_letor.py" $TEST"/test.json" "test.csv" "test.txt" $META"/"track_metadata.csv $META"/"album_metadata.csv $META"/"artist_metadata.csv $META"/"audio_metadata.csv --features $flist
