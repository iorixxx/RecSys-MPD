import json


def read_dataset_json(path):
    """ Read playlists from dataset json file
        Parameters:
            - path - absolute path of the file
    """
    with open(path, "r") as f:
        data = json.load(f)

    return data["playlists"]


def read_configuration_json(path, keys):
    """ Read and validate configuration json file
        Parameters:
            - path - absolute path of the file
            - keys - key names that must exist in the file
    """
    valid = True
    with open(path, "r") as f:
        conf = json.load(f)

    if set(conf.keys()) != keys:
        valid = False

    return valid, conf
