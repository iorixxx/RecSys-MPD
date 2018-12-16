#!/usr/bin/env bash

ID=lambdamart

EXP="/apc/experiments"
FULLEXP=$EXP"/"$ID

SRC="/apc/RecSys-MPD"
TEST="/apc/dataset/test/10fold_10K_b"
SAMPLE="/apc/sample/002"
RANKING=$SRC"/jforests/ranking2.properties"

JXMS="-Xms40g"
JXMX="-Xmx80g"

LTRLIB="jforests"


# go to source folder and pull changes from the repository
cd $SRC
git pull


# go to experiments folder, create a new folder with id
mkdir $FULLEXP
cd $FULLEXP

cp $SAMPLE"/*" $FULLEXP"/"

# apply LambdaMART: train, build model, and predict ranking scores
for i in {1..10}
do
	mkdir $i

	exc=$(( $i % 10 + 1 ))

	train=$(printf "train-%d.txt" $i)
	tst=$(printf "letor-%d.txt" $i)
	cv=$(printf "letor-%d.txt" $exc)

	train_bin=$(printf "train-%d.bin" $i)
	tst_bin=$(printf "letor-%d.bin" $i)
	cv_bin=$(printf "letor-%d.bin" $exc)

	ensemble=$(printf "ensemble-%d.txt" $i)
	predict=$(printf "predictions-%d.txt" $i)

	java $JXMS $JXMX -jar $SRC"/jforests/jforests.jar" --cmd=generate-bin --ranking --folder . --file $train --file $cv --file $tst
	java $JXMS $JXMX -jar $SRC"/jforests/jforests.jar" --cmd=train --ranking --config-file $RANKING --train-file $train_bin --validation-file $cv_bin --output-model $ensemble
	java $JXMS $JXMX -jar $SRC"/jforests/jforests.jar" --cmd=predict --ranking --model-file $ensemble --tree-type RegressionTree --test-file $tst_bin --output-file $predict

	mv jforests* $i/
	mv *.bin $i/
done


# generate re-ranked recommendations
for i in {1..10}
do
	result=$(printf "results-%d.csv" $i)
	ranked=$(printf "ranked-%d.csv" $i)
	predict=$(printf "predictions-%d.txt" $i)

	python3 $SRC"/python/submission_rank.py" $ranked $result $predict $LTRLIB
done


# evaluate
for i in {1..10}
do
	playlist=$(printf "fold-%03d.json" $i)
	result=$(printf "results-%d.csv" $i)
	ranked=$(printf "ranked-%d.csv" $i)

	python3 $SRC"/python/evaluate.py" $TEST"/"$playlist $TOPT --recommendations $result $ranked
done
