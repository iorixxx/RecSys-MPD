#!/usr/bin/env bash

SRC="/apc/RecSys-MPD"
META="/apc/metadata"

CID=""
CSEC=""

# create metadata folder
mkdir $OUT

# go to source code directory and update project
cd $SRC
git pull

# run audio feature extraction application
python3 $SRC"/python/collect_audio_features.py" $CID $CSEC $META

