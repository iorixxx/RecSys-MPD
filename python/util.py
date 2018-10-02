import json
import random

CATEGORIES = [dict(id=1, shuffle=True, fraction=0.5),
              dict(id=2, shuffle=True, fraction=0.33),
              dict(id=3, shuffle=True, fraction=0.25),
              dict(id=4, shuffle=False, fraction=0.5),
              dict(id=5, shuffle=False, fraction=0.33),
              dict(id=6, shuffle=False, fraction=0.25)]

RECSYS_CATEGORIES = [dict(id=1, seeds=0, masking=False, randomization=False),
                     dict(id=2, seeds=1, masking=False, randomization=False),
                     dict(id=3, seeds=5, masking=False, randomization=False),
                     dict(id=4, seeds=5, masking=True, randomization=False),
                     dict(id=5, seeds=10, masking=False, randomization=False),
                     dict(id=6, seeds=10, masking=True, randomization=False),
                     dict(id=7, seeds=25, masking=False, randomization=False),
                     dict(id=8, seeds=25, masking=False, randomization=True),
                     dict(id=9, seeds=100, masking=False, randomization=False),
                     dict(id=10, seeds=100, masking=False, randomization=True)]


def random_category():
    """ Return random category from CATEGORIES list
    """
    return random.choice(CATEGORIES)


def random_recsys_category(num_tracks):
    """ Read random cagtegory from RECSYS_CATEGORIES list
        Parameters:
            - num_tracks - number of tracks of current playlist
    """
    while True:
        candidate = random.choice(RECSYS_CATEGORIES)

        if num_tracks > candidate["seeds"]:
            break

    return candidate


def read_dataset_json(path):
    """ Read playlists from dataset json file
        Parameters:
            - path - absolute path of the file
    """
    with open(path, "r") as f:
        data = json.load(f)

    return data["playlists"]

