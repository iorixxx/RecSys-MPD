#!/usr/bin/env bash
rm -rf /mnt/recSys/MPD.index
cd /mnt/recSys/RecSys-MPD
mvn clean package
cd /mnt/recSys/
java -server -Xms10g -Xmx25g -jar /mnt/recSys/RecSys-MPD/target/mpd.jar /mnt/recSys/MPD.index /mnt/recSys/data/