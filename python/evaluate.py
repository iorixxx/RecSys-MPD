import sys
import csv
import json
import numpy as np
import statistics
from collections import OrderedDict

CONFIGURATION_KEYS = {"challenge_json", "recommendation_csv", "letor_recommendation_csv"}


challenges, recommendations, letor_recommendations = {}, {}, {}


def read_configuration_json(path):
    valid = True
    with open(path, "r") as f:
        global conf
        conf = json.load(f)

        if set(conf.keys()) != CONFIGURATION_KEYS:
            valid = False

    print("Configuration file is read: %s" % path)
    return valid


def evaluate(holdout_tracks, recommended_tracks):
    precision_hits, recall_hits = 0, 0

    for i in range(len(recommended_tracks)):
        track_uri = recommended_tracks[i]

        if track_uri in holdout_tracks:
            recall_hits += 1
            if i < len(holdout_tracks):
                precision_hits += 1

    return [precision_hits / len(holdout_tracks), recall_hits / len(holdout_tracks)]


def read_recommendation_csv(path, target_dict):
    with open(path, "r") as f:
        reader = csv.reader(f)
        for row in reader:
            pid = int(row[0])

            if pid not in target_dict.keys():
                target_dict[pid] = []

            target_dict[pid].append(row[1])

    print("Recommendation file is read: %s" % path)


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


def measure():
    results = {}

    for pid, challenge in challenges.items():
        category = challenge["category"]
        holdouts = challenge["holdouts"]

        try:
            predictions = recommendations[pid]
            letor_predictions = letor_recommendations[pid]
        except KeyError:
            predictions, letor_predictions = [], []

        pr = precision_recall(holdouts, predictions)
        ndcg = normalized_dcg(holdouts, predictions, 500)
        extender = playlist_extender_clicks(holdouts, predictions)

        letor_pr = precision_recall(holdouts, letor_predictions)
        letor_ndcg = normalized_dcg(holdouts, letor_predictions, 500)
        letor_extender = playlist_extender_clicks(holdouts, letor_predictions)

        if category not in results:
            results[category] = dict(precision=[], ndcg=[], extender=[], letor_precision=[], letor_ndcg=[], letor_extender=[], recall=[])

        results[category]["precision"].append(pr[0])
        results[category]["ndcg"].append(ndcg)
        results[category]["extender"].append(extender)
        results[category]["letor_precision"].append(letor_pr[0])
        results[category]["letor_ndcg"].append(letor_ndcg)
        results[category]["letor_extender"].append(letor_extender)
        results[category]["recall"].append(pr[1])

    for c in sorted(results.keys()):
        c_precision = statistics.mean(results[c]["precision"])
        c_ndcg = statistics.mean(results[c]["ndcg"])
        c_extender = statistics.mean(results[c]["extender"])

        c_letor_precision = statistics.mean(results[c]["letor_precision"])
        c_letor_ndcg = statistics.mean(results[c]["letor_ndcg"])
        c_letor_extender = statistics.mean(results[c]["letor_extender"])

        c_recall = statistics.mean(results[c]["recall"])

        print("%s\t%f\t%f\t%f\t%f\t%f\t%f\t%f" % (c, c_precision, c_ndcg, c_extender, c_letor_precision, c_letor_ndcg, c_letor_extender, c_recall))

    summarize(results)


def summarize(results):
    all_lists = [[] for i in range(7)]

    for v in results.values():
        all_lists[0].extend(v["precision"])
        all_lists[1].extend(v["ndcg"])
        all_lists[2].extend(v["extender"])
        all_lists[3].extend(v["letor_precision"])
        all_lists[4].extend(v["letor_ndcg"])
        all_lists[5].extend(v["letor_extender"])
        all_lists[6].extend(v["recall"])

    all_means = []

    for a in all_lists:
        m = statistics.mean(a)
        all_means.append(m)

    print("%s\t%f\t%f\t%f\t%f\t%f\t%f\t%f" % ("mean", all_means[0], all_means[1], all_means[2], all_means[3], all_means[4], all_means[5], all_means[6]))


if __name__ == '__main__':
    if len(sys.argv) != 2:
        print("Usage: argv0 argv1")
        print("argv1: Configuration json file")
        sys.exit(2)
    else:
        validation = read_configuration_json(sys.argv[1])

        if validation:
            read_challenge_json(conf["challenge_json"])
            read_recommendation_csv(conf["recommendation_csv"], recommendations)
            read_recommendation_csv(conf["letor_recommendation_csv"], letor_recommendations)

            measure()
        else:
            print("Configuration file cannot be validated, keys may be missing.")
