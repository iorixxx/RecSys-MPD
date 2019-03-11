import json
import random
import util
import argparse

from os import listdir
from os.path import join

CLI = argparse.ArgumentParser()

CLI.add_argument("mpd", help="Absolute path of the mpd data folder")
CLI.add_argument("output", help="Absolute path of the output folder")
CLI.add_argument("size", help="Number of playlists per category", type=int)
CLI.add_argument("instance", help="Instance of challenge", choices={"recsys", "custom"})

CUSTOM_MIN = 10

ttv = {}


def init(instance):
    if instance == "recsys":
        target = util.RECSYS_CATEGORIES
    else:
        target = util.CUSTOM_CATEGORIES

    for c in target:
        ttv[c["id"]] = []


def build(mpd_path, output_path, size, instance):
    required = len(ttv) * size

    files = listdir(mpd_path)
    random.shuffle(files)

    for file in files:
        if sum(len(v) for v in ttv.values()) == required:
            break

        print("Processing %s" % file)

        items = util.read_dataset_json(join(mpd_path, file))
        random.shuffle(items)

        for item in items:
            if instance == "recsys":
                mask_recsys(item, size)
            else:
                mask_custom(item, size)
            if sum(len(v) for v in ttv.values()) == required:
                break

    dump(output_path)


def dump(output_path):
    temp = dict(train=dict(playlists=[]), test=dict(playlists=[]), validation=dict(playlists=[]))

    for cid in ttv.keys():
        s1 = int(0.8 * len(ttv[cid]))
        s2 = int(0.9 * len(ttv[cid]))
        temp["train"]["playlists"].extend(ttv[cid][:s1])
        temp["test"]["playlists"].extend(ttv[cid][s1:s2])
        temp["validation"]["playlists"].extend(ttv[cid][s2:])

    for k, v in temp.items():
        with open(join(output_path, "%s.json" % k), "w") as out:
            json.dump(v, out, indent=4)

    print("Train, test, validation files are created in folder: %s" % output_path)


def mask_recsys(item, size):
    pid = item["pid"]
    num_tracks = item["num_tracks"]

    categories = util.filter_recsys_categories(num_tracks)
    try:
        category = next(c for c in categories if len(ttv[c["id"]]) < size)
        cid = category["id"]
        num_samples = category["seeds"]

        playlist_json = dict(pid=pid, category=cid, num_tracks=num_tracks, num_samples=num_samples)

        if category["title"]:
            playlist_json["name"] = item["name"]

        if category["shuffle"]:
            random.shuffle(item["tracks"])

        playlist_json["num_holdouts"] = num_tracks - num_samples
        playlist_json["tracks"] = sorted(item["tracks"][0:num_samples], key=lambda x: x["pos"], reverse=False)
        playlist_json["holdouts"] = sorted(item["tracks"][num_samples:], key=lambda x: x["pos"], reverse=False)

        ttv[cid].append(playlist_json)
    except StopIteration:
        print("No categorical space left for pid:%d" % pid)


def mask_custom(item, size):
    pid = item["pid"]
    num_tracks = item["num_tracks"]

    if num_tracks > CUSTOM_MIN:
        category = next(c for c in util.CUSTOM_CATEGORIES if len(ttv[c["id"]]) < size)
        cid = category["id"]

        playlist_json = dict(pid=pid, category=cid, name=item["name"], num_tracks=num_tracks)

        if category["shuffle"]:
            random.shuffle(item["tracks"])

        num_samples = int(num_tracks * category["fraction"])

        playlist_json["num_samples"] = num_samples
        playlist_json["num_holdouts"] = num_tracks - num_samples
        playlist_json["tracks"] = sorted(item["tracks"][0:num_samples], key=lambda x: x["pos"], reverse=False)
        playlist_json["holdouts"] = sorted(item["tracks"][num_samples:], key=lambda x: x["pos"], reverse=False)

        ttv[cid].append(playlist_json)
    else:
        print("Too few tracks in pid:%d" % pid)


if __name__ == '__main__':
    args = CLI.parse_args()
    print(args)

    init(instance=args.instance)
    build(mpd_path=args.mpd, output_path=args.output, size=args.size, instance=args.instance)
