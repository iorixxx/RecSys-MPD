# A multi-stage retrieval approach to playlist continuation: An Information Retrieval Perspective

For the task of automatic playlist continuation described in [RecSys Challenge 2018](https://recsys-challenge.spotify.com), we propose a multi-stage IR (Information Retrieval) approach combined with LTR (Learning to Rank). 

Our model is capable of generating a list of recommended tracks for a given playlist which has a number of withheld tracks. Briefly, we follow three main steps to produce relevant results:
1. Indexing: We build a Lucene index over the MPD. 
2. Searching: By treating playlists as documents and tracks as queries, we search for possible candidates over the index for an incomplete playlist. 
3. Ranking: We apply LambdaMART algorithm to re-rank the list of candidate tracks. 

During the ranking step, we use statistical features derived from the MPD. Optionally, we can also utilize audio and album features collected via [Spotify Web API](https://developer.spotify.com/documentation/web-api/). Thus, our model can compete in both main and creative tracks of the challenge.

In order to reproduce our results, one can run ```reproduce.sh``` script provided in the repository. This script builds everything needed from scratch, assuming that requirements below are available at the computing environment:
* Java
* Maven
* Python 3.5 or later

