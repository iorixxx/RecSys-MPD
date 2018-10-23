#!/usr/bin/env bash

SRC="/apc/RecSys-MPD" # source code directory
META="/apc/metadata" # metadata directory

CID="" # spotify developer client ID
CSEC="" # spotify developer client secret


# go to source code directory and update project
cd $SRC
git pull

# run audio feature extraction application
python3 $SRC"/python/collect_audio_features.py" $CID $CSEC $META
