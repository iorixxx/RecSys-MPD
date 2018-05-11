#!/usr/bin/env bash

java -server -Xms10g -Xmx25g -cp /mnt/recSys/RecSys-MPD/target/mpd.jar edu.anadolu.app.FillerApp /mnt/recSys/MPD.index /mnt/recSys/challenge_set.json "$1"