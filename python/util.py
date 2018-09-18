import json


def read_dataset_json(path):
    """ Read playlists from a dataset json file
        Parameters:
            - path - absolute path of the file
    """
    with open(path, "r") as f:
        data = json.load(f)
    return data["playlists"]

