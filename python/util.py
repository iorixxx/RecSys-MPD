import json
import random

CUSTOM_CATEGORIES = [dict(id=1, shuffle=True, fraction=0.50, display="Random 50%"),
                     dict(id=2, shuffle=True,  fraction=0.33, display="Random 33%"),
                     dict(id=3, shuffle=True,  fraction=0.25, display="Random 25%"),
                     dict(id=4, shuffle=True,  fraction=0.10, display="Random 10%"),
                     dict(id=5, shuffle=False, fraction=0.50, display="First 50%"),
                     dict(id=6, shuffle=False, fraction=0.33, display="First 33%"),
                     dict(id=7, shuffle=False, fraction=0.25, display="First 25%"),
                     dict(id=8, shuffle=False, fraction=0.10, display="First 10%")]

RECSYS_CATEGORIES = [dict(id=1,  shuffle=False, seeds=0,   title=True,  lower=10, upper=50, display="Title only"),
                     dict(id=2,  shuffle=False, seeds=1,   title=True,  lower=10, upper=78, display="First 1"),
                     dict(id=3,  shuffle=False, seeds=5,   title=True,  lower=10, upper=100, display="First 5"),
                     dict(id=4,  shuffle=False, seeds=5,   title=False, lower=40, upper=100, display="First 5 without title"),
                     dict(id=5,  shuffle=False, seeds=10,  title=True,  lower=40, upper=100, display="First 10"),
                     dict(id=6,  shuffle=False, seeds=10,  title=False, lower=40, upper=100, display="First 10 without title"),
                     dict(id=7,  shuffle=False, seeds=25,  title=True,  lower=101, upper=250, display="First 25"),
                     dict(id=8,  shuffle=True,  seeds=25,  title=True,  lower=101, upper=250, display="Random 25"),
                     dict(id=9,  shuffle=False, seeds=100, title=True,  lower=150, upper=250, display="First 100"),
                     dict(id=10, shuffle=True,  seeds=100, title=True,  lower=150, upper=250, display="Random 100")]


def random_category():
    """ Return random category from CATEGORIES list
    """
    return random.choice(CUSTOM_CATEGORIES)


def search_category(instance, cid):
    """ Search for a category with specific id and instance
    """
    if instance == "recsys":
        return next(item for item in RECSYS_CATEGORIES if item["id"] == cid)
    else:
        return next(item for item in CUSTOM_CATEGORIES if item["id"] == cid)


def filter_recsys_categories(length):
    return [item for item in RECSYS_CATEGORIES if item["lower"] <= length <= item["upper"]]


def read_dataset_json(path):
    """ Read playlists from dataset json file
        Parameters:
            - path - absolute path of the file
    """
    with open(path, "r") as f:
        data = json.load(f)

    return data["playlists"]
