#!/usr/bin/env bash
cd /mnt/recSys/RecSys-MPD
#git pull
mvn clean package
cd /mnt/recSys/
java -server -Xms10g -Xmx25g -cp /mnt/recSys/RecSys-MPD/target/mpd.jar edu.anadolu.app.SearchApp /mnt/recSys/MPD.index /mnt/recSys/challenge_set.json dph.csv RECSYS DPH false Mode3
python verify_submission.py challenge_set.json submission.csv