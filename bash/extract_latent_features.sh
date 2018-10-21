#!/usr/bin/env bash

SRC="/apc/RecSys-MPD"
MPD="/apc/dataset/mpd/data"
OUT="/apc/metadata"

# create metadata folder
mkdir $OUT

# go to source code directory and update project
cd $SRC
git pull

# run latent feature extraction application
python3 $SRC"/python/collect_latent_features.py" $MPD $OUT
