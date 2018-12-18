#!/usr/bin/env bash

ID=100

EXP="/apc/experiments"
FULLEXP=$EXP"/"$ID

SRC="/apc/RecSys-MPD"
TEST="/apc/dataset/test/10fold_10K_b"
SAMPLE="/apc/sample/001"
RANKING=$SRC"/jforests/ranking2.properties"

JXMS="-Xms40g"
JXMX="-Xmx80g"

LTRLIB="jforests"

CUTOFF=50

SIZES=(50 100 150 200 250 300 350 400 450 500)


# go to source folder and pull changes from the repository
cd $SRC
git pull


# create a new folder with id
mkdir $FULLEXP


# go to samples folder and copy files
cd $SAMPLE
cp * $FULLEXP"/"


# go to experiments folder
cd $FULLEXP


# apply LambdaMART: train, build model, and predict ranking scores
for k in "${SIZES[@]}"
do
	EXP=$SAMPLE"/"$k
	cd $EXP
	cp $SRC"/jforests/jforests.jar" "runnable.jar" 
	
	for i in {1..10}
	do
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

		rm jforests*
		rm *.bin
	done
done


# generate re-ranked recommendations
for k in "${SIZES[@]}"
do
	EXP=$SAMPLE"/"$k
	cd $EXP

	for i in {1..10}
	do
		result=$(printf "results-%d.csv" $i)
		ranked=$(printf "ranked-%d.csv" $i)
		predict=$(printf "predictions-%d.txt" $i)

		python3 $SRC"/python/submission_rank.py" $ranked $result $predict $LTRLIB
	done
done


# evaluate
for k in "${SIZES[@]}"
do
	EXP=$SAMPLE"/"$k
	cd $EXP
	
	for i in {1..10}
	do
		echo $k"-"$i
	
		playlist=$(printf "fold-%03d.json" $i)
		result=$(printf "results-%d.csv" $i)
		ranked=$(printf "ranked-%d.csv" $i)

		python3 $SRC"/python/evaluate.py" $TEST"/"$playlist $CUTOFF --recommendations $result $ranked
	done
done
