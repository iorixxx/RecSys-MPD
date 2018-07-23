# A multi-stage retrieval approach to playlist continuation: An Information Retrieval Perspective

For the task of automatic playlist continuation described in [RecSys Challenge 2018](https://recsys-challenge.spotify.com), we propose a multi-stage IR (Information Retrieval) approach combined with LTR (Learning to Rank). 

Our model is capable of generating a list of recommended tracks for a given playlist which has a number of withheld tracks. Briefly, we follow three main steps to produce relevant results:
1. Indexing: We build a Lucene index over the MPD. 
2. Searching: By treating playlists as documents and tracks as queries, we search for possible candidates over the index for an incomplete playlist. 
3. Ranking: We apply LambdaMART algorithm to re-rank the list of candidate tracks. 

During the ranking step, we use statistical features derived from the MPD. Optionally, we can also utilize audio and album features collected via [Spotify Web API](https://developer.spotify.com/documentation/web-api/). Thus, our model can compete in both main and creative tracks of the challenge. Following table presents the complete set of features:

| Feature ID | Description                                          | Category | Track           |
| :---       | :---                                                 | :---     | :---            |
| 1	         | Number of tracks in playlist	                        | P        | Main & Creative |
| 2	         | Number of samples in playlist	                      | P        | Main & Creative |
| 3	         | Number of holdouts in playlist	                      | P        | Main & Creative |
| 4	         | Length of playlist title	                            | P        | Main & Creative |
| 5	         | Track occurrence	                                    | T        | Main & Creative |
| 6	         | Track frequency	                                    | T        | Main & Creative |
| 7	         | Track duration	                                      | T        | Main & Creative |
| 8	         | Album occurrence	                                    | T        | Main & Creative |
| 9	         | Album frequency	                                    | T        | Main & Creative |
| 10	       | Number of tracks in album	                          | T        | Main & Creative |
| 11	       | Artist occurrence	                                  | T        | Main & Creative |
| 12	       | Artist frequency	                                    | T        | Main & Creative |
| 13	       | Number of albums of artist	                          | T        | Main & Creative |
| 14	       | Number of tracks of artist	                          | T        | Main & Creative |
| 15	       | Submission order	                                    | PxT      | Main & Creative |
| 16	       | Lucene score	                                        | PxT      | Main & Creative |
| 17	       | Predicted position	                                  | PxT      | Main & Creative |
| 18	       | Jaccard distance of playlist title and track name	  | PxT      | Main & Creative |
| 19	       | Jaccard distance of playlist title and album name	  | PxT      | Main & Creative |
| 20	       | Jaccard distance of playlist title and artist name	  | PxT      | Main & Creative |
| 21	       | Audio danceability	                                  | T        | Creative Only   |
| 22	       | Audio energy	                                        | T        | Creative Only   |
| 23	       | Audio key	                                          | T        | Creative Only   |
| 24	       | Audio loudness	                                      | T        | Creative Only   |
| 25	       | Audio mode	                                          | T        | Creative Only   |
| 26	       | Audio speechiness	                                  | T        | Creative Only   |
| 27	       | Audio acousticness	                                  | T        | Creative Only   |
| 28	       | Audio instrumentalness	                              | T        | Creative Only   |
| 29	       | Audio liveness	                                      | T        | Creative Only   |
| 30	       | Audio valence	                                      | T        | Creative Only   |
| 31	       | Audio tempo	                                        | T        | Creative Only   |
| 32	       | Audio time signature	                                | T        | Creative Only   |
| 33	       | Album popularity	                                    | T        | Creative Only   |

In order to reproduce our results, one can run ```reproduce.sh``` script provided in the repository. This script builds everything needed from scratch, assuming that requirements below are available at the computing environment:
* JDK 9 or higher
* Maven 3
* Python 3.5 or later

### Team Info
The name of our team in the challenge is **Anadolu**. The members of our team are:
* Ahmet Arslan
* Alper Bilge
* Ali Yürekli
