#!/usr/bin/env bash

ID=0

EXP="/apc/experiments"

FULLEXP=$EXP"/"$ID

SRC="/apc/RecSys-MPD"
TEST="/apc/dataset/test/1fold_1M"
INDEX="/apc/MPD.index"

JXMS="-Xms80g"
JXMX="-Xmx120g"

TOPK=200
TOPT=500

SIMILARITIES=( BM25 IB DFI PL2 DLM DPH )
SORTERS=( LuceneSort FreqSort GeoSort )
FIELDS=( Track Album Artist Whole )

# go to source folder, pull changes from the repository, and build project
cd $SRC
git pull
mvn clean package

# go to experiments folder, create a new folder with id
mkdir $FULLEXP
cd $FULLEXP

# generate sampling data, evaluate, and remove dumps
for i in {1..10}
do
	ifold=$(printf "fold-%03d.json" $i)

	for similarity in "${SIMILARITIES[@]}"
	do
		for sorter in "${SORTERS[@]}"
		do
			for field in "${FIELDS[@]}"
			do
				result=$similarity"-"$sorter"-"$field".csv"
				
				java -server $JXMS $JXMX -cp $SRC"/target/mpd.jar" edu.anadolu.app.BestSearchApp $INDEX $TEST"/"$ifold $result $similarity $TOPK $TOPT $sorter $field		

				echo $ifold
				echo $result
			
				python3 $SRC"/python/evaluate.py" $TEST"/"$ifold $TOPT --recommendations $result
				
				rm $result
			done
		done
	done
done

