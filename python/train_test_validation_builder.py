import json
import random
import sys
from os import listdir
from os.path import join


DATA_DIR, DUMP_DIR = None, None

categories = {1: dict(name="cat1", seeds=0, masking=False, randomization=False),
              2: dict(name="cat2", seeds=1, masking=False, randomization=False),
              3: dict(name="cat3", seeds=5, masking=False, randomization=False),
              4: dict(name="cat4", seeds=5, masking=True, randomization=False),
              5: dict(name="cat5", seeds=10, masking=False, randomization=False),
              6: dict(name="cat6", seeds=10, masking=True, randomization=False),
              7: dict(name="cat7", seeds=25, masking=False, randomization=False),
              8: dict(name="cat8", seeds=25, masking=False, randomization=True),
              9: dict(name="cat9", seeds=100, masking=False, randomization=False),
              10: dict(name="cat10", seeds=100, masking=False, randomization=True)}


dump_dict = dict(playlists=list())


def select_random_category(num_tracks):
    while True:
        k, v = random.choice(list(categories.items()))
        if num_tracks > v["seeds"]:
            break

    return k, v


def process_dataset_json(path):
    with open(path, "r") as f:
        data = json.load(f)

        for playlist in data["playlists"]:
            num_tracks = playlist["num_tracks"]

            cat_k, cat_v = select_random_category(num_tracks)

            num_samples = cat_v["seeds"]
            num_holdouts = num_tracks - num_samples

            playlist_json = dict(pid=playlist["pid"])

            if cat_v["masking"] is False:
                playlist_json["name"] = playlist["name"]

            playlist_json["category"] = cat_v["name"]
            playlist_json["num_tracks"] = num_tracks
            playlist_json["num_samples"] = num_samples
            playlist_json["num_holdouts"] = num_holdouts

            if cat_v["randomization"] is False:
                playlist_json["tracks"] = playlist["tracks"][0:num_samples]
                playlist_json["holdouts"] = playlist["tracks"][num_samples:]
            else:
                temp = playlist["tracks"]
                random.shuffle(temp)
                playlist_json["tracks"] = sorted(temp[0:num_samples], key=lambda x: x["pos"], reverse=False)
                playlist_json["holdouts"] = sorted(temp[num_samples:], key=lambda x: x["pos"], reverse=False)

            dump_dict["playlists"].append(playlist_json)


def dump(train, test, validation):
    items = dump_dict["playlists"]
    random.shuffle(items)

    a = len(items)
    a1 = int(a * train / 100)
    a2 = a1 + int(a * test / 100)

    train_set = dict(playlists=items[0:a1])
    test_set = dict(playlists=items[a1:a2])
    validation_set = dict(playlists=items[a2:])

    with open(join(DUMP_DIR, "train.json"), "w") as f:
        json.dump(train_set, f, indent=4)

    with open(join(DUMP_DIR, "test.json"), "w") as f:
        json.dump(test_set, f, indent=4)

    with open(join(DUMP_DIR, "validation.json"), "w") as f:
        json.dump(validation_set, f, indent=4)

    print("Train:%d Test:%d Validation:%d split is ready" % (train, test, validation))


def validate(train, test, validation):
    return train > 0 and test > 0 and validation > 0 and train + test + validation == 100


if __name__ == '__main__':
    if len(sys.argv) != 6:
        print("Usage: argv0 argv1 argv2 argv3 argv4 arg5")
        print("argv1: The MPD data folder path")
        print("argv2: Output folder path")
        print("argv3: Train")
        print("argv4: Test")
        print("argv5: Validation")
        sys.exit(2)
    else:
        DATA_DIRECTORY = sys.argv[1]
        DUMP_DIR = sys.argv[2]
        train_x = int(sys.argv[3])
        test_x = int(sys.argv[4])
        validation_x = int(sys.argv[5])

        if validate(train_x, test_x, validation_x):
            for file in listdir(DATA_DIRECTORY):
                print("Processing %s" % file)
                process_dataset_json(join(DATA_DIRECTORY, file))

            dump(train_x, test_x, validation_x)
        else:
            print("Please check split values")
            sys.exit(2)