#!/usr/bin/env bash
java -server -Xms1g -Xmx2g -cp target/mpd.jar edu.anadolu.app.SearchApp /Users/iorixxx/Desktop/MPD.index/ /Users/iorixxx/Desktop/challenge.v1/challenge_set.json submission.csv RECSYS BM25 Blended false Mode3