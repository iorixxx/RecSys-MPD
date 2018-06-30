#!/usr/bin/env bash

# initialize paths for the MPD and challenge set
MPD_PATH=/mnt/recSys/dataset/mpd/data
CHALLENGE_PATH=/mnt/recSys/dataset/challenge/challenge_set.json


# Spotify Web API credentials must be set here to compete in creative track
CLIENT_ID=
CLIENT_SECRET=


# create a new folder for the experiment
rm -rf experiment
mkdir experiment
cd experiment


# checkout the source code and build 
git clone https://github.com/iorixxx/RecSys-MPD.git
cd RecSys-MPD
mvn clean package
cd ..


# create a new folder runnables and copy applications to this folder
mkdir runnable
cp RecSys-MPD/target/mpd.jar runnable/
cp RecSys-MPD/python/* runnable/
cp RecSys-MPD/jforests/jforests.jar runnable/
cp RecSys-MPD/jforests/ranking.properties .

# create index for the MPD
java -server -Xms10g -Xmx20g -jar runnable/mpd.jar MPD.index $MPD_PATH

# build dataset according to train test validation split
MAX_JSON_FILES_OF_MPD=100
TRAIN_SPLIT=80
TEST_SPLIT=10
VALIDATION_SPLIT=10

python3 runnable/train_test_validation_builder.py $MPD_PATH . $MAX_JSON_FILES_OF_MPD $TRAIN_SPLIT $TEST_SPLIT $VALIDATION_SPLIT


# search for relevant tracks for train, test, validation json files (use test for local evaluation, challenge set for Spotify evaluation)
java -server -Xms16g -Xmx32g -cp runnable/mpd.jar edu.anadolu.app.SearchApp MPD.index train.json train.csv RECSYS PL2 Blended false score-train.csv true &
java -server -Xms16g -Xmx32g -cp runnable/mpd.jar edu.anadolu.app.SearchApp MPD.index $CHALLENGE_PATH test.csv RECSYS PL2 Blended false score-test.csv true &
java -server -Xms16g -Xmx32g -cp runnable/mpd.jar edu.anadolu.app.SearchApp MPD.index validation.json validation.csv RECSYS PL2 Blended false score-validation.csv true &
wait


# build metadata for LTR (audio features and album popularities are optional for creative track)
mkdir metadata
python3 runnable/metadata_builder.py $MPD_PATH metadata/
python3 runnable/collect_audio_features.py metadata/track_metadata.csv metadata/audio_metadata.csv $CLIENT_ID $CLIENT_SECRET
python3 runnable/collect_album_popularity.py metadata/album_metadata.csv metadata/album_poularity.csv $CLIENT_ID $CLIENT_SECRET


# build LTR model (Last parameter of submission_to_letor.py indicates track mode, False=main, True=creative)

python3 runnable/submission_to_letor.py train.json metadata/track_metadata.csv metadata/album_metadata.csv metadata/artist_metadata.csv metadata/audio_metadata.csv metadata/album_popularity.csv train.csv score-train.csv train.txt False
python3 runnable/submission_to_letor.py test.json metadata/track_metadata.csv metadata/album_metadata.csv metadata/artist_metadata.csv metadata/audio_metadata.csv metadata/album_popularity.csv test.csv score-test.csv test.txt False
python3 runnable/submission_to_letor.py validation.json metadata/track_metadata.csv metadata/album_metadata.csv metadata/artist_metadata.csv metadata/audio_metadata.csv metadata/album_popularity.csv validation.csv score-validation.csv validation.txt False

java -server -Xms16g -Xmx64g -jar runnable/jforests.jar --cmd=generate-bin --ranking --folder . --file train.txt --file validation.txt --file test.txt
java -server -Xms16g -Xmx64g -jar runnable/jforests.jar --cmd=train --ranking --config-file ranking.properties --train-file train.bin --validation-file validation.bin --output-model ensemble.txt
java -server -Xms16g -Xmx64g -jar runnable/jforests.jar --cmd=predict --ranking  --model-file ensemble.txt --tree-type RegressionTree --test-file test.bin --output-file predictions.txt


# re-rank recommended tracks
python3 /mnt/recSys/runnable/submission_rank.py test.txt predictions.txt sorted_test.csv

