import json
import random

CATEGORIES = [dict(id=1, type="random", shuffle=True, fraction=0.5),
              dict(id=2, type="random", shuffle=True, fraction=0.33),
              dict(id=3, type="random", shuffle=True, fraction=0.25),
              dict(id=4, type="random", shuffle=True, fraction=0.1),
              dict(id=5, type="firstN", shuffle=False, fraction=0.5),
              dict(id=6, type="firstN", shuffle=False, fraction=0.33),
              dict(id=7, type="firstN", shuffle=False, fraction=0.25),
              dict(id=8, type="firstN", shuffle=False, fraction=0.1)]


def random_category():
    """ Return random category from CATEGORIES list
    """
    return random.choice(CATEGORIES)


def search_category(cid):
    """ Search for a category with specific id within CATEGORIES list
    """
    return next(item for item in CATEGORIES if item["id"] == cid)


def read_dataset_json(path):
    """ Read playlists from dataset json file
        Parameters:
            - path - absolute path of the file
    """
    with open(path, "r") as f:
        data = json.load(f)

    return data["playlists"]
