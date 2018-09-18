import json
import sys
import random
import util

from os import listdir
from os.path import join

CONFIGURATION_KEYS = {"mpd_directory", "output_directory", "k"}

MPD_SIZE = 1000000


def build(mpd_path, output_path, k):
    count, current_k, partition_size = 0, 1, int(MPD_SIZE / k)

    files = listdir(mpd_path)
    random.shuffle(files)

    challenges = dict(playlists=[])

    for file in files:
        print("Processing %s" % file)

        items = util.read_dataset_json(join(mpd_path, file))
        random.shuffle(items)

        for item in items:
            count += 1
            playlist_json = dict(pid=item["pid"], name=item["name"], category="cat1")

            random.shuffle(item["tracks"])

            num_tracks = len(item["tracks"])
            num_samples = int(num_tracks / 2)

            playlist_json["num_tracks"] = num_tracks
            playlist_json["num_samples"] = num_samples
            playlist_json["num_holdouts"] = num_tracks - num_samples

            playlist_json["tracks"] = sorted(item["tracks"][0:num_samples], key=lambda x: x["pos"], reverse=False)
            playlist_json["holdouts"] = sorted(item["tracks"][num_samples:], key=lambda x: x["pos"], reverse=False)

            challenges["playlists"].append(playlist_json)

            if count == partition_size:
                with open(join(output_path, "fold-{0:03d}.json".format(current_k)), "w") as f:
                    json.dump(challenges, f, indent=4)

                print("Fold %d is created with %d playlists" % (current_k, len(challenges["playlists"])))

                count = 0
                current_k += 1

                del challenges["playlists"][:]

    print("%d-fold cv files are created in folder: %s" % (k, output_path))


if __name__ == '__main__':
    if len(sys.argv) != 2:
        print("Usage: argv0 argv1")
        print("argv1: Configuration json file")
        sys.exit(2)
    else:
        validation, conf = util.read_configuration_json(sys.argv[1], CONFIGURATION_KEYS)

        if validation:
            build(mpd_path=conf["mpd_directory"], output_path=conf["output_directory"], k=conf["k"])
        else:
            print("Configuration file cannot be validated, following keys must be satisfied.")
            print(CONFIGURATION_KEYS)
