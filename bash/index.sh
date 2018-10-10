#!/usr/bin/env bash

INDEX="/apc/MPD.index"
SRC="/apc/RecSys-MPD"
MPD="/apc/dataset/mpd/data"

# remove previously created index
rm -rf $INDEX

# go to source code directory, update project, build project
cd $SRC
git pull
mvn clean package

# run indexer application
java -server -Xms20g -Xmx50g -jar target/mpd.jar $INDEX $MPD
