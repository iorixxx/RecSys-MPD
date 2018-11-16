import json
import random

CATEGORIES = [dict(id=1, shuffle=True, fraction=0.5),
              dict(id=2, shuffle=True, fraction=0.33),
              dict(id=3, shuffle=True, fraction=0.25)]

RECSYS_CATEGORIES = [dict(id=1, min_boundary=10, max_boundary=50, seeds=0, masking=False, randomization=False),
                     dict(id=2, min_boundary=10, max_boundary=78, seeds=1, masking=False, randomization=False),
                     dict(id=3, min_boundary=10, max_boundary=100, seeds=5, masking=False, randomization=False),
                     dict(id=4, min_boundary=40, max_boundary=100, seeds=5, masking=True, randomization=False),
                     dict(id=5, min_boundary=40, max_boundary=100, seeds=10, masking=False, randomization=False),
                     dict(id=6, min_boundary=40, max_boundary=100, seeds=10, masking=True, randomization=False),
                     dict(id=7, min_boundary=101, max_boundary=250, seeds=25, masking=False, randomization=False),
                     dict(id=8, min_boundary=101, max_boundary=250, seeds=25, masking=False, randomization=True),
                     dict(id=9, min_boundary=150, max_boundary=250, seeds=100, masking=False, randomization=False),
                     dict(id=10, min_boundary=150, max_boundary=250, seeds=100, masking=False, randomization=True)]


def random_category():
    """ Return random category from CATEGORIES list
    """
    return random.choice(CATEGORIES)


def random_recsys_category(num_tracks):
    """ Read random category from RECSYS_CATEGORIES list
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

