#!/usr/bin/env bash

SRC="/apc/RecSys-MPD"
OUT="/apc/metadata"
TMETA="/apc/metadata/track_metadata.csv"

CID=""
CSEC=""

# create metadata folder
mkdir $OUT

# go to source code directory and update project
cd $SRC
git pull

# run audio feature extraction application
python3 $SRC"/python/collect_audio_features.py" $CID $CSEC $OUT $TMETA
