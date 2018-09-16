import json
import sys
import random

from os import listdir
from os.path import join

CONFIGURATION_KEYS = {"mpd_directory", "output_directory", "k"}

challenges = {}


def read_configuration_json(path):
    valid = True
    with open(path, "r") as f:
        global conf
        conf = json.load(f)

        if set(conf.keys()) != CONFIGURATION_KEYS:
            valid = False

    print("Configuration file is read: %s" % path)
    return valid


def read_dataset_json(path):
    with open(path, "r") as f:
        data = json.load(f)

    return data["playlists"]


def export():
    for k, v in sorted(challenges.items()):
        with open(join(conf["output_directory"], "fold-{0:03d}.json".format(k)), "w") as f:
            json.dump(v, f, indent=4)
        print("Playlists in fold %d: %d" % (k, len(v["playlists"])))


def build():
    k, turn = int(conf["k"]), 0

    files = listdir(conf["mpd_directory"])
    random.shuffle(files)

    for file in files:
        print("Processing %s" % file)

        items = read_dataset_json(join(conf["mpd_directory"], file))
        random.shuffle(items)

        for item in items:
            turn += 1
            playlist_json = dict(pid=item["pid"], name=item["name"], category="cat1")

            random.shuffle(item["tracks"])

            num_tracks = len(item["tracks"])
            num_samples = int(num_tracks / 2)

            playlist_json["num_tracks"] = num_tracks
            playlist_json["num_samples"] = num_samples
            playlist_json["num_holdouts"] = num_tracks - num_samples

            playlist_json["tracks"] = sorted(item["tracks"][0:num_samples], key=lambda x: x["pos"], reverse=False)
            playlist_json["holdouts"] = sorted(item["tracks"][num_samples:], key=lambda x: x["pos"], reverse=False)

            if turn not in challenges:
                challenges[turn] = dict(playlists=[])

            challenges[turn]["playlists"].append(playlist_json)

            if turn % k == 0:
                turn = 0

    export()
    print("Challenge files are created in: %s" % conf["output_directory"])


if __name__ == '__main__':
    if len(sys.argv) != 2:
        print("Usage: argv0 argv1")
        print("argv1: Configuration json file")
        sys.exit(2)
    else:
        validation = read_configuration_json(sys.argv[1])

        if validation:
            build()
        else:
            print("Configuration file cannot be validated, keys may be missing.")
