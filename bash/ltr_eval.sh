#!/usr/bin/env bash

EXP="/apc/experiments"

SRC="/apc/RecSys-MPD"
TEST="/apc/dataset/test/10fold_10K_b"
SAMPLE="/apc/sample/002"

ALGS=(mart adarank ranknet lambdamart)

CUTOFFS=(50 100 150 200 250 300 350 400 450 500)

# evaluate
for alg in "${ALGS[@]}"
do
	
	FULLEXP=$EXP"/"$alg
	cd $FULLEXP

	for cutoff in "${CUTOFFS[@]}"
	do
		echo $alg"-"$cutoff
	
		for i in {1..10}
		do
			playlist=$(printf "fold-%03d.json" $i)
			result=$SAMPLE"/"$(printf "results-%d.csv" $i)
			ranked=$(printf "ranked-%d.csv" $i)

			python3 $SRC"/python/evaluate.py" $TEST"/"$playlist $cutoff --recommendations $result $ranked
		done
	done
done
