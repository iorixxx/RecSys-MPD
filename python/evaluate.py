import sys
import csv
import json
import numpy as np
import statistics
from collections import OrderedDict

CONFIGURATION_KEYS = {"challenge_json", "recommendation_csv_list"}

challenges = {}


def read_configuration_json(path):
    valid = True
    with open(path, "r") as f:
        global conf
        conf = json.load(f)

        if set(conf.keys()) != CONFIGURATION_KEYS or \
                ("recommendation_csv_list" in conf.keys() and len(conf["recommendation_csv_list"]) == 0):
            valid = False

    print("Configuration file is read: %s" % path)
    return valid


def read_recommendation_csv(path):
    target_dict = {}
    with open(path, "r") as f:
        reader = csv.reader(f)
        for row in reader:
            pid = int(row[0])

            if pid not in target_dict.keys():
                target_dict[pid] = []

            target_dict[pid].append(row[1])

    print("Recommendation file is read: %s" % path)
    return target_dict


def read_challenge_json(path):
    with open(path, "r") as f:
        data_holder = json.load(f)

        for item in data_holder["playlists"]:
            pid = item["pid"]
            category = int(item["category"].replace("cat", ""))
            holdouts = []

            for h in item["holdouts"]:
                holdouts.append(h["track_uri"])

            challenges[pid] = dict(category=category, holdouts=holdouts)

    print("Challenge file is read: %s" % path)


# evaluating precision and recall
def precision_recall(targets, predictions, max_n_predictions=500):
    predictions = predictions[:max_n_predictions]

    target_set = set(targets)
    target_count = len(target_set)

    p = float(len(set(predictions[:target_count]).intersection(target_set))) / target_count
    r = float(len(set(predictions).intersection(target_set))) / target_count

    return p, r


# evaluating dcg
def dcg(relevant_elements, retrieved_elements, k):
    relevant_elements = list(OrderedDict.fromkeys(relevant_elements[:k]))
    retrieved_elements = list(OrderedDict.fromkeys(retrieved_elements))

    if len(relevant_elements) == 0 or len(retrieved_elements) == 0:
        return 0.0

    score = [float(el in relevant_elements) for el in retrieved_elements]

    return np.sum(np.divide(score, np.log2(1 + np.arange(1, len(score) + 1))))


# evaluating ndcg
def normalized_dcg(relevant_elements, retrieved_elements, k):
    idcg = dcg(relevant_elements, relevant_elements, min(k, len(relevant_elements)))

    if idcg == 0:
        raise ValueError("relevant_elements is empty, the metric is not defined")

    true_dcg = dcg(relevant_elements, retrieved_elements, k)
    return true_dcg / idcg


# evaluating recommended songs clicks
def playlist_extender_clicks(targets, predictions, max_n_predictions=500):
    predictions = predictions[:max_n_predictions]

    i = set(predictions).intersection(set(targets))
    for index, t in enumerate(predictions):
        for track in i:
            if t == track:
                return float(int(index / 10))

    return float(max_n_predictions / 10.0 + 1)


def measure(path):
    print("Evaluating recommendation file: %s" % path)
    results = {}
    recommendations = read_recommendation_csv(path)

    for pid, challenge in challenges.items():
        category = challenge["category"]
        holdouts = challenge["holdouts"]

        try:
            predictions = recommendations[pid]
        except KeyError:
            predictions = []

        pr = precision_recall(holdouts, predictions)
        ndcg = normalized_dcg(holdouts, predictions, 500)
        extender = playlist_extender_clicks(holdouts, predictions)

        if category not in results:
            results[category] = dict(precision=[], recall=[], ndcg=[], extender=[])

        results[category]["precision"].append(pr[0])
        results[category]["recall"].append(pr[1])
        results[category]["ndcg"].append(ndcg)
        results[category]["extender"].append(extender)

    for c in sorted(results.keys()):
        c_precision = statistics.mean(results[c]["precision"])
        c_recall = statistics.mean(results[c]["recall"])
        c_ndcg = statistics.mean(results[c]["ndcg"])
        c_extender = statistics.mean(results[c]["extender"])

        print("%s\t%f\t%f\t%f\t%f" % (c, c_precision, c_recall, c_ndcg, c_extender))


if __name__ == '__main__':
    if len(sys.argv) != 2:
        print("Usage: argv0 argv1")
        print("argv1: Configuration json file")
        sys.exit(2)
    else:
        validation = read_configuration_json(sys.argv[1])

        if validation:
            read_challenge_json(conf["challenge_json"])

            for recommendation_csv in conf["recommendation_csv_list"]:
                measure(recommendation_csv)
        else:
            print("Configuration file cannot be validated, keys may be missing.")