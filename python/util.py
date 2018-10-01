import json
import random

CATEGORIES = [dict(id=1, shuffle=True, fraction=0.5),
              dict(id=2, shuffle=True, fraction=0.33),
              dict(id=3, shuffle=True, fraction=0.25),
              dict(id=4, shuffle=False, fraction=0.5),
              dict(id=5, shuffle=False, fraction=0.33),
              dict(id=6, shuffle=False, fraction=0.25)]


def random_category():
    """ Return random category from CATEGORIES list
    """
    return random.choice(CATEGORIES)


def read_dataset_json(path):
    """ Read playlists from dataset json file
        Parameters:
            - path - absolute path of the file
    """
    with open(path, "r") as f:
        data = json.load(f)

    return data["playlists"]

