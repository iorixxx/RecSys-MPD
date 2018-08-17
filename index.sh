#!/usr/bin/env bash

# remove previously created index
rm -rf /home/recsys2018/apc/MPD.index

# go to source code directory, update project, build project
cd /home/recsys2018/apc/RecSys-MPD
git pull
mvn clean package

# run indexer application
java -server -Xms10g -Xmx25g -jar target/mpd.jar /home/recsys2018/apc/MPD.index /home/recsys2018/apc/dataset/mpd/data
