#!/usr/bin/env bash

ID=001
SAMPLE="/apc/sample/"$ID

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


# go to source folder, pull changes from the repository, and build project
cd $SRC
git pull
mvn clean package


# create and go to sampling folder
mkdir $SAMPLE
cd $SAMPLE


# generate sampling data
java -server $JXMS $JXMX -cp $SRC"/target/mpd.jar" edu.anadolu.app.BestSearchApp $INDEX $TEST"/train.json" "train.csv" $SIMILARITY $TOPK $TOPT $SORTER $SEARCHFIELD
java -server $JXMS $JXMX -cp $SRC"/target/mpd.jar" edu.anadolu.app.BestSearchApp $INDEX $TEST"/validation.json" "validation.csv" $SIMILARITY $TOPK $TOPT $SORTER $SEARCHFIELD
java -server $JXMS $JXMX -cp $SRC"/target/mpd.jar" edu.anadolu.app.BestSearchApp $INDEX $TEST"/test.json" "test.csv" $SIMILARITY $TOPK $TOPT $SORTER $SEARCHFIELD