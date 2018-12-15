#!/usr/bin/env bash

ID=adarank

EXP="/apc/experiments"

FULLEXP=$EXP"/"$ID

SRC="/apc/RecSys-MPD"
TEST="/apc/dataset/test/10fold_10K_b"
SAMPLE="/apc/sample/002"

JXMS="-Xms40g"
JXMX="-Xmx80g"

LTRLIB="ranklib"

TOPK=200
TOPT=500
CUTOFF=500

RANKER=3
METRIC="NDCG@"$CUTOFF


# go to source folder and pull changes from the repository
cd $SRC
git pull


# go to experiments folder, create a new folder with id
mkdir $FULLEXP
cd $FULLEXP


# apply AdaRank: train, build model, and predict ranking scores
for i in {1..10}
do
	exc=$(( $i % 10 + 1 ))

	train=$SAMPLE"/"$(printf "train-%d.txt" $i)
	tst=$SAMPLE"/"$(printf "letor-%d.txt" $i)
	cv=$SAMPLE"/"$(printf "letor-%d.txt" $exc)

	ensemble=$(printf "ensemble-%d.txt" $i)
	predict=$(printf "predictions-%d.txt" $i)


	java $JXMS $JXMX -jar $SRC"/ranklib/RankLib.jar" -train $train -test $tst -validate $cv -ranker $RANKER -metric2t $METRIC -metric2T $METRIC -save $ensemble
	java $JXMS $JXMX -jar $SRC"/ranklib/RankLib.jar" -load $ensemble -rank $tst -score $predict
done


# generate re-ranked recommendations
for i in {1..10}
do
	result=$SAMPLE"/"$(printf "results-%d.csv" $i)
	ranked=$(printf "ranked-%d.csv" $i)
	predict=$(printf "predictions-%d.txt" $i)

	python3 $SRC"/python/submission_rank.py" $ranked $result $predict $LTRLIB
done


# evaluate
for i in {1..10}
do
	playlist=$(printf "fold-%03d.json" $i)
	result=$SAMPLE"/"$(printf "results-%d.csv" $i)
	ranked=$(printf "ranked-%d.csv" $i)

	python3 $SRC"/python/evaluate.py" $TEST"/"$playlist $CUTOFF --recommendations $result $ranked
done
