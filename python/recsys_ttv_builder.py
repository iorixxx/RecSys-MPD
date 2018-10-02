import json
import random
import argparse
import util

from os import listdir
from os.path import join

CLI = argparse.ArgumentParser()

CLI.add_argument("mpd", help="Absolute path of the mpd data folder")
CLI.add_argument("output", help="Absolute path of the output folder")
CLI.add_argument("limit", help="Maximum number of json files to use", type=int)


dump_dict = dict(playlists=list())


def process_dataset_json(path):
    playlists = util.read_dataset_json(path)

    for playlist in playlists:
        num_tracks = playlist["num_tracks"]

        category = util.random_recsys_category(num_tracks)

        num_samples = category["seeds"]
        num_holdouts = num_tracks - num_samples

        playlist_json = dict(pid=playlist["pid"])

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

        dump_dict["playlists"].append(playlist_json)


def dump(path, train=80, test=10, validation=10):
    items = dump_dict["playlists"]
    random.shuffle(items)

    a = len(items)
    a1 = int(a * train / 100)
    a2 = a1 + int(a * test / 100)

    train_set = dict(playlists=items[0:a1])
    test_set = dict(playlists=items[a1:a2])
    validation_set = dict(playlists=items[a2:])

    with open(join(path, "train.json"), "w") as f:
        json.dump(train_set, f, indent=4)

    with open(join(path, "test.json"), "w") as f:
        json.dump(test_set, f, indent=4)

    with open(join(path, "validation.json"), "w") as f:
        json.dump(validation_set, f, indent=4)

    print("Train:%d Test:%d Validation:%d split is ready" % (train, test, validation))


def build(mpd_path, output_path, limit):
    files = listdir(mpd_path)
    random.shuffle(files)

    for file in files[0:limit]:
        print("Processing %s" % file)
        process_dataset_json(path=join(mpd_path, file))

    dump(output_path)


if __name__ == '__main__':
    args = CLI.parse_args()

    build(mpd_path=args.mpd, output_path=args.output, limit=args.limit)
