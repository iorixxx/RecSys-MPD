import json
import csv
import sys
from os import listdir
from os.path import join

track_metadata, album_metadata, artist_metadata = {}, {}, {}


def process_dataset_json(path):
    with open(path, "r") as f:
        data = json.load(f)

        for playlist in data["playlists"]:
            pid = playlist["pid"]

            for track in playlist["tracks"]:
                track_uri = track["track_uri"]
                album_uri = track["album_uri"]
                artist_uri = track["artist_uri"]

                if track_uri not in track_metadata.keys():
                    track_metadata[track_uri] = dict(track_name=track["track_name"], album_uri=album_uri, artist_uri=artist_uri, duration=track["duration_ms"], occurrence=1, pids={pid})
                else:
                    track_metadata[track_uri]["occurrence"] = track_metadata[track_uri]["occurrence"] + 1
                    track_metadata[track_uri]["pids"].add(pid)

                if album_uri not in album_metadata.keys():
                    album_metadata[album_uri] = dict(album_name=track["album_name"], tracks={track_uri}, occurrence=1, pids={pid})
                else:
                    album_metadata[album_uri]["occurrence"] = album_metadata[album_uri]["occurrence"] + 1
                    album_metadata[album_uri]["tracks"].add(track_uri)
                    album_metadata[album_uri]["pids"].add(pid)

                if artist_uri not in artist_metadata.keys():
                    artist_metadata[artist_uri] = dict(artist_name=track["artist_name"], albums={album_uri}, tracks={track_uri}, occurrence=1, pids={pid})
                else:
                    artist_metadata[artist_uri]["occurrence"] = artist_metadata[artist_uri]["occurrence"] + 1
                    artist_metadata[artist_uri]["albums"].add(album_uri)
                    artist_metadata[artist_uri]["tracks"].add(track_uri)
                    artist_metadata[artist_uri]["pids"].add(pid)


def build():
    for file in listdir(DATA_DIRECTORY):
        print("Processing %s" % file)
        process_dataset_json(join(DATA_DIRECTORY, file))

    with open(join(METADATA_DIRECTORY, "track_metadata.csv"), "w", newline='', encoding="utf-8") as f:
        writer = csv.writer(f)
        for k, v in track_metadata.items():
            writer.writerow([k, v["occurrence"], len(v["pids"]), v["duration"], v["album_uri"], v["artist_uri"], v["track_name"]])

    with open(join(METADATA_DIRECTORY, "album_metadata.csv"), "w", newline='', encoding="utf-8") as f:
        writer = csv.writer(f)
        for k, v in album_metadata.items():
            writer.writerow([k, v["occurrence"], len(v["pids"]), len(v["tracks"]), v["album_name"]])

    with open(join(METADATA_DIRECTORY, "artist_metadata.csv"), "w", newline='', encoding="utf-8") as f:
        writer = csv.writer(f)
        for k, v in artist_metadata.items():
            writer.writerow([k, v["occurrence"], len(v["pids"]), len(v["albums"]), len(v["tracks"]), v["artist_name"]])


if __name__ == '__main__':
    if len(sys.argv) != 3:
        print("Usage: argv0 argv1 argv2")
        print("argv1: The MPD data folder path")
        print("argv2: Metadata folder path")
        sys.exit(2)
    else:
        DATA_DIRECTORY = sys.argv[1]
        METADATA_DIRECTORY = sys.argv[2]

        build()
