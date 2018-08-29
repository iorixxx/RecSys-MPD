#!/usr/bin/env bash

# go to source code directory, update project, build project
cd /home/recsys2018/apc/RecSys-MPD
git pull
mvn clean package

# run searcher application
java -server -Xms10g -Xmx20g -cp target/mpd.jar edu.anadolu.app.SearchApp /mnt/MPD.index /home/recsys2018/apc/dataset/test/test-001.json dph.csv DPH false Mode3 true