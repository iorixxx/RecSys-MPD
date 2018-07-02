import sys
import csv
import spotipy
from spotipy.oauth2 import SpotifyClientCredentials

MAX_BATCH = 50


def extract_tracks():
    tracks = []

    with open(TRACK_METADATA_PATH, "r", encoding="utf-8") as f1:
        reader = csv.reader(f1)
        for row in reader:
            tracks.append(row[0].split(":")[2])

    print("Track metadata file is read: %s" % TRACK_METADATA_PATH)
    return tracks


def count_batches(t):
    length_t = len(t)

    if length_t % MAX_BATCH == 0:
        return int(length_t / MAX_BATCH)
    else:
        return int(length_t / MAX_BATCH) + 1


def collect():
    client_credentials_manager = SpotifyClientCredentials(client_id=CLIENT_ID, client_secret=CLIENT_SECRET)
    sp = spotipy.Spotify(client_credentials_manager=client_credentials_manager)

    tracks = extract_tracks()
    batches = count_batches(tracks)

    with open(AUDIO_FEATURES_PATH, "w", newline='') as f:
        writer = csv.writer(f)

        for i in range(batches):
            print("Batch %d" % i)
            sub_list = tracks[i*MAX_BATCH : (i+1)*MAX_BATCH]
            audios = sp.audio_features(sub_list)

            for audio in audios:
                try:
                    writer.writerow([audio["uri"], audio["danceability"], audio["energy"], audio["key"], audio["loudness"], audio["mode"], audio["speechiness"],
                                     audio["acousticness"], audio["instrumentalness"], audio["liveness"], audio["valence"], audio["tempo"], audio["time_signature"]])
                except TypeError:
                    print("Audio features of a track cannot be obtained")

    print("Audio features are collected: %s" % AUDIO_FEATURES_PATH)


if __name__ == '__main__':
    if len(sys.argv) != 5:
        print("Usage: argv0 argv1 argv2 argv3 argv4")
        print("argv1: track metadata csv file")
        print("argv2: audio features output csv file")
        print("argv3: Client ID")
        print("argv4: Client Secret")
        sys.exit(2)
    else:
        TRACK_METADATA_PATH = sys.argv[1]
        AUDIO_FEATURES_PATH = sys.argv[2]
        CLIENT_ID = sys.argv[3]
        CLIENT_SECRET = sys.argv[4]

        collect()