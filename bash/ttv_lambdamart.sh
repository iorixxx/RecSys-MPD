#!/usr/bin/env bash

ID=001
SAMPLE="/apc/sample/"$ID
FEATURE="/apc/feature/"$ID

EXP="/apc/experiments/lambdamart"

SRC="/apc/RecSys-MPD"
TEST="/apc/dataset/test/ttv100K"
RANKING=$SRC"/jforests/ranking2.properties"

JXMS="-Xms40g"
JXMX="-Xmx80g"

LTRLIB="jforests"

CUTOFF=500


# go to source folder and pull changes from the repository
cd $SRC
git pull


# create and go to experiments folder
mkdir $EXP
cd $EXP


# apply LambdaMART: train, build model, and predict ranking scores
train=$FEATURE"/train.txt"
tst=$FEATURE"/test.txt"
cv=$FEATURE"/validation.txt"

train_bin="train.bin"
tst_bin="test.bin"
cv_bin="validation.bin"

ensemble="ensemble.txt"
predict="predictions.txt"

java $JXMS $JXMX -jar $SRC"/jforests/jforests.jar" --cmd=generate-bin --ranking --folder . --file $train --file $cv --file $tst
java $JXMS $JXMX -jar $SRC"/jforests/jforests.jar" --cmd=train --ranking --config-file $RANKING --train-file $train_bin --validation-file $cv_bin --output-model $ensemble
java $JXMS $JXMX -jar $SRC"/jforests/jforests.jar" --cmd=predict --ranking --model-file $ensemble --tree-type RegressionTree --test-file $tst_bin --output-file $predict


# generate re-ranked recommendations
original=$SAMPLE"/test.csv"
ranked="ranked.csv"

python3 $SRC"/python/submission_rank.py" $ranked $original $predict $LTRLIB


# evaluate
playlist="test.json"

python3 $SRC"/python/evaluate.py" $TEST"/"$playlist $CUTOFF --recommendations $original $ranked

