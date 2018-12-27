#!/usr/bin/env bash

ID=001
SAMPLE="/apc/sample/"$ID

EXP="/apc/experiments/randomforests"

SRC="/apc/RecSys-MPD"
TEST="/apc/dataset/test/ttv100K"

JXMS="-Xms40g"
JXMX="-Xmx80g"

CUTOFF=500


# go to source folder and pull changes from the repository
cd $SRC
git pull


# evaluate with t-test
playlist=$TEST"/test.json"
original=$SAMPLE"/test.csv"
ranked=$EXP"/ranked.csv"

python3 $SRC"/python/evaluate.py" $playlist $CUTOFF --recommendations $original $ranked --log $EXP
