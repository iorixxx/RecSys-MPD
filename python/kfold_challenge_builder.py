import json
import random
import util
import argparse

from os import listdir
from os.path import join

MPD_SIZE = 1000000

CLI = argparse.ArgumentParser()

CLI.add_argument("k", help="Number of folds")
CLI.add_argument("mpd", help="Absolute path of the mpd data folder")
CLI.add_argument("output", help="Absolute path of the output folder")


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
            category = util.random_category()
            playlist_json = dict(pid=item["pid"], name=item["name"], category=category["id"])

            if category["shuffle"]:
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
    args = CLI.parse_args()

    build(mpd_path=args.mpd, output_path=args.output, k=int(args.k))
