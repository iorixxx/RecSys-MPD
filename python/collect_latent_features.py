import csv
import util
import argparse

from os import listdir
from os.path import join


CLI = argparse.ArgumentParser()

CLI.add_argument("mpd", help="Absolute path of the mpd data folder")
CLI.add_argument("output", help="Absolute path of the output folder")

track_metadata, album_metadata, artist_metadata = {}, {}, {}


def process_dataset_json(path):
    playlists = util.read_dataset_json(path)

    for playlist in playlists:
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


def collect(mpd_directory, output_directory):

    for file in listdir(mpd_directory):
        print("Processing %s" % file)
        process_dataset_json(join(mpd_directory, file))

    with open(join(output_directory, "track_metadata.csv"), "w", newline='', encoding="utf-8") as f:
        writer = csv.writer(f)
        for k, v in track_metadata.items():
            writer.writerow([k, v["occurrence"], len(v["pids"]), v["duration"], v["album_uri"], v["artist_uri"], v["track_name"]])

    with open(join(output_directory, "album_metadata.csv"), "w", newline='', encoding="utf-8") as f:
        writer = csv.writer(f)
        for k, v in album_metadata.items():
            writer.writerow([k, v["occurrence"], len(v["pids"]), len(v["tracks"]), v["album_name"]])

    with open(join(output_directory, "artist_metadata.csv"), "w", newline='', encoding="utf-8") as f:
        writer = csv.writer(f)
        for k, v in artist_metadata.items():
            writer.writerow([k, v["occurrence"], len(v["pids"]), len(v["albums"]), len(v["tracks"]), v["artist_name"]])


if __name__ == '__main__':
    args = CLI.parse_args()

    collect(mpd_directory=args.mpd, output_directory=args.output)
