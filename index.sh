#!/usr/bin/env bash

# remove previously created index
rm -rf /mnt/MPD.index

# go to source code directory, update project, build project
cd /home/recsys2018/apc/RecSys-MPD
git pull
mvn clean package

# run indexer application
java -server -Xms20g -Xmx50g -jar target/mpd.jar /mnt/MPD.index /home/recsys2018/apc/dataset/mpd/data
