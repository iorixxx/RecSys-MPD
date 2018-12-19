import json
import random
import util
import argparse

from os import listdir
from os.path import join

CLI = argparse.ArgumentParser()

CLI.add_argument("mpd", help="Absolute path of the mpd data folder")
CLI.add_argument("output", help="Absolute path of the output folder")
CLI.add_argument("size", help="Number of playlists to include", type=int)

index = {
    0: "validation",
    1: "train",
    2: "train",
    3: "train",
    4: "train",
    5: "train",
    6: "train",
    7: "train",
    8: "train",
    9: "test",
}


def build(mpd_path, output_path, size):
    count = 0

    files = listdir(mpd_path)
    random.shuffle(files)

    ttv = dict(train=dict(playlists=[]), test=dict(playlists=[]), validation=dict(playlists=[]))

    for file in files:
        if count >= size:
            break

        print("Processing %s" % file)

        items = util.read_dataset_json(join(mpd_path, file))
        random.shuffle(items)

        for item in items:
            category = util.random_category()
            playlist_json = dict(pid=item["pid"], name=item["name"], category=category["id"])

            if category["shuffle"]:
                random.shuffle(item["tracks"])

            num_tracks = len(item["tracks"])
            num_samples = int(num_tracks * category["fraction"])

            playlist_json["num_tracks"] = num_tracks
            playlist_json["num_samples"] = num_samples
            playlist_json["num_holdouts"] = num_tracks - num_samples

            playlist_json["tracks"] = sorted(item["tracks"][0:num_samples], key=lambda x: x["pos"], reverse=False)
            playlist_json["holdouts"] = sorted(item["tracks"][num_samples:], key=lambda x: x["pos"], reverse=False)

            ttv[index[count % 10]]["playlists"].append(playlist_json)

            count += 1

            if count >= size:
                break

    for k, v in ttv.items():
        with open(join(output_path, "%s.json" % k), "w") as out:
            json.dump(v, out, indent=4)

    print("Train, test, validation files are created in folder: %s" % output_path)


if __name__ == '__main__':
    args = CLI.parse_args()

    build(mpd_path=args.mpd, output_path=args.output, size=args.size)
