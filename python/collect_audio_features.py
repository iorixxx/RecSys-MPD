import sys
import csv
import json
import spotipy
from spotipy.oauth2 import SpotifyClientCredentials

CONFIGURATION_KEYS = {"track_metadata_csv", "output_csv", "client_id", "client_secret"}

MAX_BATCH = 50


def read_configuration_json(path):
    valid = True
    with open(path, "r") as f:
        global conf
        conf = json.load(f)

        if set(conf.keys()) != CONFIGURATION_KEYS:
            valid = False

    print("Configuration file is read: %s" % path)
    return valid


def extract_tracks(path):
    tracks = []

    with open(path, "r", encoding="utf-8") as f1:
        reader = csv.reader(f1)
        for row in reader:
            tracks.append(row[0].split(":")[2])

    print("Track metadata file is read: %s" % path)
    return tracks


def count_batches(t):
    length_t = len(t)

    if length_t % MAX_BATCH == 0:
        return int(length_t / MAX_BATCH)
    else:
        return int(length_t / MAX_BATCH) + 1


def collect():
    client_credentials_manager = SpotifyClientCredentials(client_id=conf["client_id"], client_secret=conf["client_secret"])
    sp = spotipy.Spotify(client_credentials_manager=client_credentials_manager)

    tracks = extract_tracks(conf["track_metadata_csv"])
    batches = count_batches(tracks)

    with open(conf["output_csv"], "w", newline='') as f:
        writer = csv.writer(f)

        for i in range(batches):
            print("Batch %d" % i)
            sub_list = tracks[i*MAX_BATCH: (i+1)*MAX_BATCH]
            audios = sp.audio_features(sub_list)

            for audio in audios:
                try:
                    writer.writerow([audio["uri"], audio["danceability"], audio["energy"], audio["key"], audio["loudness"], audio["mode"], audio["speechiness"],
                                     audio["acousticness"], audio["instrumentalness"], audio["liveness"], audio["valence"], audio["tempo"], audio["time_signature"]])
                except TypeError:
                    print("Audio features of a track cannot be obtained")

    print("Audio features are collected: %s" % conf["output_csv"])


if __name__ == '__main__':
    if len(sys.argv) != 2:
        print("Usage: argv0 argv1")
        print("argv1: Configuration json file")
        sys.exit(2)
    else:
        validation = read_configuration_json(sys.argv[1])

        if validation:
            collect()
        else:
            print("Configuration file cannot be validated, keys may be missing.")