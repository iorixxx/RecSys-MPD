import json
import random
import argparse
import util

from os import listdir
from os.path import join


CLI = argparse.ArgumentParser()

CLI.add_argument("mpd", help="Absolute path of the mpd data folder")
CLI.add_argument("output", help="Absolute path of the output folder")
CLI.add_argument("instances", help="Number of instances in a category", type=int)
CLI.add_argument("--categories", help="RecSys categories to be included in data", nargs="+", type=int, required=False)

SPLITS = {1: dict(ratio=0.8, filename="train.json"),
          2: dict(ratio=0.1, filename="test.json"),
          3: dict(ratio=0.1, filename="validation.json")}


def export(path, data):
    with open(path, "w") as out:
        json.dump(data, out, indent=4)
    print("RecSys challenge file is created: %s" % path)


def obtain_playlist_json(playlist, category):
    playlist_json = dict(pid=playlist["pid"])

    num_tracks = playlist["num_tracks"]
    num_samples = category["seeds"]
    num_holdouts = num_tracks - num_samples

    if category["masking"] is False:
        playlist_json["name"] = playlist["name"]

    playlist_json["category"] = category["id"]
    playlist_json["num_tracks"] = num_tracks
    playlist_json["num_samples"] = num_samples
    playlist_json["num_holdouts"] = num_holdouts

    if category["randomization"] is False:
        playlist_json["tracks"] = playlist["tracks"][0:num_samples]
        playlist_json["holdouts"] = playlist["tracks"][num_samples:]
    else:
        temp = playlist["tracks"]
        random.shuffle(temp)
        playlist_json["tracks"] = sorted(temp[0:num_samples], key=lambda x: x["pos"], reverse=False)
        playlist_json["holdouts"] = sorted(temp[num_samples:], key=lambda x: x["pos"], reverse=False)

    return playlist_json


def build(mpd_path, output_path, categories, instances):
    files = listdir(mpd_path)
    random.shuffle(files)

    for split in SPLITS.values():
        dumpy = dict(playlists=list())

        for category in util.RECSYS_CATEGORIES:
            if category["id"] not in categories:
                continue

            cnt, required = 0, instances * split["ratio"]
            while True:
                file = files.pop(0)
                print("Processing %s" % file)

                playlists = util.read_dataset_json(join(mpd_path, file))

                for playlist in playlists:
                    if not category["min_boundary"] <= playlist["num_tracks"] <= category["max_boundary"]:
                        continue

                    playlist_json = obtain_playlist_json(playlist, category)
                    dumpy["playlists"].append(playlist_json)

                    cnt += 1
                    if cnt == required:
                        break
                if cnt == required:
                    break

        export(join(output_path, split["filename"]), dumpy)


if __name__ == '__main__':
    args = CLI.parse_args()

    build(mpd_path=args.mpd, output_path=args.output, categories=args.categories, instances=args.instances)
