#!/usr/bin/env bash

ID=0

EXP="/apc/experiments"

FULLEXP=$EXP"/"$ID

SRC="/apc/RecSys-MPD"
TEST="/apc/dataset/test/1fold_1M/fold-001.json"
INDEX="/apc/MPD.index"

JXMS="-Xms40g"
JXMX="-Xmx80g"

TOPK=200
TOPT=500

SIMILARITIES=( BM25 DFIC DLM DPH LGD PL2 )
SORTERS=( LuceneSort FreqSort GeoSort )
FIELDS=( Track Album Artist Whole )

# go to source folder, pull changes from the repository, and build project
cd $SRC
git pull
mvn clean package

# go to experiments folder, create a new folder with id
mkdir $FULLEXP
cd $FULLEXP

# generate sampling data

for similarity in "${SIMILARITIES[@]}"
do
	for sorter in "${SORTERS[@]}"
	do
		for field in "${FIELDS[@]}"
		do
			result=$similarity"-"$sorter"-"$field".csv"
			
			java -server $JXMS $JXMX -cp $SRC"/target/mpd.jar" edu.anadolu.app.BestSearchApp $INDEX $TEST $result $similarity $TOPK $TOPT $sorter $field		
		done
	done
done


