#!/usr/bin/env bash

ID=003

SAMPLE="/apc/sample/aliyurekli/Desktop/sample/"$ID
EXP="/apc/experiments/fsimportance"
SRC="/apc/RecSys-MPD"


# go to source folder and pull changes from the repository
cd $SRC
git pull


tst=$SAMPLE"/test.csv"

python3 $SRC"/python/gas_feature_similarity.py" $tst $EXP 38