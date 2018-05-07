#!/usr/bin/env bash
cd ~/RecSys-MPD
#git pull
mvn clean package
cd ~/
java -server -Xms5g -Xmx5g -cp ~/RecSys-MPD/target/mpd.jar edu.anadolu.Feature ~/data/
wc -l Features.txt