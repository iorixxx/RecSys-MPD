#!/usr/bin/env bash

EXP="/apc/experiments/fsimportance"
SRC="/apc/RecSys-MPD"


# go to source folder and pull changes from the repository
cd $SRC
git pull

python3 $SRC"/python/gas_feature_similarity.py" $EXP 38