#!/usr/bin/env bash

# go to root directory and create metadata folder
cd /home/recsys2018/apc
mkdir metadata

# go to source code directory and update project
cd /home/recsys2018/apc/RecSys-MPD
git pull
cd python

# run audio feature extraction application
python3 collect_audio_features.py /home/recsys2018/apc/pyconfig/collect_audio_features.json
