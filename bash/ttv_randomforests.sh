#!/usr/bin/env bash

ID=001
SAMPLE="/apc/sample/"$ID
FEATURE="/apc/feature/"$ID

EXP="/apc/experiments/randomforests"

SRC="/apc/RecSys-MPD"
TEST="/apc/dataset/test/ttv100K"

JXMS="-Xms40g"
JXMX="-Xmx80g"

LTRLIB="ranklib"

CUTOFF=500
METRIC="NDCG@"$CUTOFF

RANKER=8

# go to source folder and pull changes from the repository
cd $SRC
git pull


# create and go to experiments folder
mkdir $EXP
cd $EXP


# apply RankNet: train, build model, and predict ranking scores
train=$FEATURE"/train.txt"
tst=$FEATURE"/test.txt"
cv=$FEATURE"/validation.txt"

ensemble="ensemble.txt"
predict="predictions.txt"

java $JXMS $JXMX -jar $SRC"/ranklib/RankLib.jar" -train $train -test $tst -validate $cv -ranker $RANKER -metric2t $METRIC -metric2T $METRIC -save $ensemble
java $JXMS $JXMX -jar $SRC"/ranklib/RankLib.jar" -load $ensemble -rank $tst -score $predict


# generate re-ranked recommendations
original=$SAMPLE"/test.csv"
ranked="ranked.csv"

python3 $SRC"/python/submission_rank.py" $ranked $original $predict $LTRLIB


# evaluate
playlist="test.json"

python3 $SRC"/python/evaluate.py" $TEST"/"$playlist $CUTOFF --recommendations $original $ranked
