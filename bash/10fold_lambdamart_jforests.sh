
#!/usr/bin/env bash

ID=0

EXP="/apc/experiments"

FULLEXP=$EXP"/"$ID

META="/apc/metadata"
SRC="/apc/RecSys-MPD"
TEST="/apc/dataset/test/10fold_10K_b"
INDEX="/apc/MPD.index"
RANKING=$SRC"/jforests/ranking2.properties"

JXMS="-Xms40g"
JXMX="-Xmx80g"

SIMILARITY="BM25"
SORTER="NoSort"
SEARCHFIELD="Track"

LTRLIB="jforests"

TOPK=200
TOPT=500

FEATURES=(1 2 3 4 5 6 7 8 9 10 11 12 13 14 15 16 17 18 19 20 21 22 23 24 25 26 27 28 29 30 31 32 33 34)


# go to source folder, pull changes from the repository, and build project
cd $SRC
git pull
mvn clean package


# go to experiments folder, create a new folder with id
mkdir $FULLEXP
cd $FULLEXP


# generate sampling data
for i in {1..10}
do
	playlist=$(printf "fold-%03d.json" $i)
	result=$(printf "results-%d.csv" $i)

	java -server $JXMS $JXMX -cp $SRC"/target/mpd.jar" edu.anadolu.app.BestSearchApp $INDEX $TEST"/"$playlist $result $SIMILARITY $TOPK $TOPT $SORTER $SEARCHFIELD
done


# extract learning-to-rank features
for i in {1..10}
do
	playlist=$(printf "fold-%03d.json" $i)
	result=$(printf "results-%d.csv" $i)
	letor=$(printf "letor-%d.txt" $i)

	flist=$(printf " %d" "${FEATURES[@]}")
	flist=${flist:1}

	python3 $SRC"/python/submission_to_letor.py" $TEST"/"$playlist $result $letor $META"/"track_metadata.csv $META"/"album_metadata.csv $META"/"artist_metadata.csv $META"/"audio_metadata.csv --features $flist
done


# merge feature partitions into separate training files
for i in {1..10}
do
	exc=$(( $i % 10 + 1 ))

	array=(1 2 3 4 5 6 7 8 9 10)

	new_array=()

	for value in "${array[@]}"
	do
		[[ $value != $i && $value != $exc ]] && new_array+=($value)
	done

	array=("${new_array[@]}")
	unset new_array

	args=$(printf " letor-%d.txt" "${array[@]}")
	args=${args:1}

	output=$(printf "train-%d.txt" $i)

	python3 $SRC"/python/merge_letor_files.py" $output --letors $args
done


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
