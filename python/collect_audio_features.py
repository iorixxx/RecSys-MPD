import csv
import spotipy
import argparse

from spotipy.oauth2 import SpotifyClientCredentials

MAX_BATCH = 50

CLI = argparse.ArgumentParser()

CLI.add_argument("clientId", help="Spotify client id")
CLI.add_argument("clientSecret", help="Spotify client secret")
CLI.add_argument("output", help="Absolute path of the output csv file")
CLI.add_argument("trackMeta", help="Absolute path of the track metadata csv file")


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


def collect(track_metadata_path, output_path, client_id, client_secret):
    client_credentials_manager = SpotifyClientCredentials(client_id=client_id, client_secret=client_secret)
    sp = spotipy.Spotify(client_credentials_manager=client_credentials_manager)

    tracks = extract_tracks(track_metadata_path)
    batches = count_batches(tracks)

    with open(output_path, "w", newline='') as f:
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

    print("Audio features are collected: %s" % output_path)


if __name__ == '__main__':
    args = CLI.parse_args()

    collect(track_metadata_path=args.trackMeta, output_path=args.output, client_id=args.clientId, client_secret=args.clientSecret)
