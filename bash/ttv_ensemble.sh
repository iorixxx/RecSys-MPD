
#!/usr/bin/env bash

ID=0
EXP="/apc/experiments/"$ID

META="/apc/metadata"
SRC="/apc/RecSys-MPD"
TEST="/apc/dataset/test/ttv100K"
INDEX="/apc/MPD.index"

JXMS="-Xms40g"
JXMX="-Xmx80g"

SIMILARITY="PL2"
SORTER="GeoSort"
SEARCHFIELD="Track"

LTRLIB="jforests"

TOPK=200
TOPT=500

CUTOFF=500

FEATURES=(1 2 3 4 5 6 7 8 9 10 11 12 13 14 15 16 17 18 19 20 21 22 23 24 25 26 27 28 29 30 31 32 33 34)


# go to source folder, pull changes from the repository, and build project
cd $SRC
git pull
mvn clean package


# go to experiments folder, create a new folder with id
mkdir $EXP
cd $EXP

playlist="test.json"
results="test.csv"
letor="test.txt"
letor_bin="test.bin"


# generate sampling data
java -server $JXMS $JXMX -cp $SRC"/target/mpd.jar" edu.anadolu.app.BestSearchApp $INDEX $TEST"/"$playlist $results $SIMILARITY $TOPK $TOPT $SORTER $SEARCHFIELD


# extract learning-to-rank features
flist=$(printf " %d" "${FEATURES[@]}")
flist=${flist:1}

python3 $SRC"/python/submission_to_letor.py" $TEST"/"$playlist $results $letor $META"/"track_metadata.csv $META"/"album_metadata.csv $META"/"artist_metadata.csv $META"/"audio_metadata.csv --features $flist
java $JXMS $JXMX -jar $SRC"/jforests/jforests.jar" --cmd=generate-bin --ranking --folder . --file $letor

# apply LambdaMART ensemble: predict ranking scores and generate ranked recommendations

for i in {1..4}
do
	ensemble=$SRC"/"$(printf "ensemble-%03d.txt" $i)
	predict=$(printf "predictions-%03d.txt" $i)
	ranked=$(printf "ranked-%03d.csv" $i)

	java $JXMS $JXMX -jar $SRC"/jforests/jforests.jar" --cmd=predict --ranking --model-file $ensemble --tree-type RegressionTree --test-file $letor_bin --output-file $predict
	python3 $SRC"/python/submission_rank.py" $ranked $results $predict $LTRLIB
done


# evaluate
for i in {1..4}
do
	ranked=$(printf "ranked-%03d.csv" $i)

	python3 $SRC"/python/evaluate.py" $TEST"/"$playlist $CUTOFF --recommendations $results $ranked
done


