import csv
import util
import numpy as np
import statistics
import argparse

from os.path import join
from collections import OrderedDict
from tabulate import tabulate

CLI = argparse.ArgumentParser()

CLI.add_argument("fold", help="Absolute path of the fold json file")
CLI.add_argument("k", help="Maximum number of recommended tracks", type=int)
CLI.add_argument("--recommendations", help="Absolute paths of recommendation csv files", nargs="+", required=True)
CLI.add_argument("--log", help="Absolute path of output t-test directory", required=False)


METRICS = ["precision", "recall", "ndcg", "extender"]

challenges = {}


def read_recommendation_csv(path):
    target_dict = {}
    with open(path, "r") as f:
        reader = csv.reader(f)
        for row in reader:
            pid = int(row[0])

            if pid not in target_dict.keys():
                target_dict[pid] = []

            target_dict[pid].append(row[1])

    return target_dict


def read_challenge_json(path):
    playlists = util.read_dataset_json(path)

    for playlist in playlists:
        pid = playlist["pid"]
        category = int(playlist["category"])
        holdouts = []

        for h in playlist["holdouts"]:
            holdouts.append(h["track_uri"])

        challenges[pid] = dict(category=category, holdouts=holdouts)

    print("\nChallenge file is read: %s" % path)


# evaluating precision and recall
def precision_recall(targets, predictions, k):
    predictions = predictions[:k]

    target_set = set(targets)
    target_count = len(target_set)

    p = float(len(set(predictions[:target_count]).intersection(target_set))) / target_count
    r = float(len(set(predictions).intersection(target_set))) / target_count

    return p, r


# evaluating dcg
def dcg(relevant_elements, retrieved_elements, k):
    retrieved_elements = list(OrderedDict.fromkeys(retrieved_elements[:k]))
    relevant_elements = list(OrderedDict.fromkeys(relevant_elements))

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
def playlist_extender_clicks(targets, predictions, k):
    predictions = predictions[:k]

    i = set(predictions).intersection(set(targets))
    for index, t in enumerate(predictions):
        for track in i:
            if t == track:
                return float(int(index / 10))

    return float(k / 10.0 + 1)


def measure(path1, path2, k, term):
    results, logs = {}, {}
    recommendations = read_recommendation_csv(path1)

    for pid, challenge in challenges.items():
        category = challenge["category"]
        holdouts = challenge["holdouts"]

        try:
            predictions = recommendations[pid]
        except KeyError:
            predictions = []

        pr = precision_recall(holdouts, predictions, k)
        ndcg = normalized_dcg(holdouts, predictions, k)
        extender = playlist_extender_clicks(holdouts, predictions, k)

        if category not in results:
            results[category] = dict(precision=[], recall=[], ndcg=[], extender=[])

        results[category]["precision"].append(pr[0])
        results[category]["recall"].append(pr[1])
        results[category]["ndcg"].append(ndcg)
        results[category]["extender"].append(extender)

        if logging:
            logs[pid] = (pr[0], pr[1], ndcg, extender)

    summary, m_instances = [], 0

    for c in sorted(results.keys()):
        v1, v2, v3, v4 = results[c]["precision"], results[c]["recall"], results[c]["ndcg"], results[c]["extender"]

        c_precision = statistics.mean(v1)
        c_recall = statistics.mean(v2)
        c_ndcg = statistics.mean(v3)
        c_extender = statistics.mean(v4)

        c_instances = len(v1)
        m_instances += c_instances

        summary.append([c, c_instances, c_precision, c_recall, c_ndcg, c_extender])

    m = compute_overall_mean(results)
    summary.append(["mean", m_instances, m[0], m[1], m[2], m[3]])

    if logging:
        export_logs(path=join(path2, "logs-%d.txt" % (term+1)), scores=logs)

    return summary


def compute_overall_mean(results):
    metrics = ["precision", "recall", "ndcg", "extender"]
    m = [[0, 0] for _ in range(4)]

    for c in results.keys():
        for i in range(len(metrics)):
            m[i][0] += sum(results[c][metrics[i]])
            m[i][1] += len(results[c][metrics[i]])

    return [x[0] / x[1] for x in m]


def show_results(summary):
    v = []

    for i in range(len(summary[0])):
        line = []

        for j in range(len(summary)):
            if j == 0:
                line.extend(summary[j][i])
            else:
                line.extend(summary[j][i][2:])

        v.append(line)

    print(tabulate(v, headers=["category", "instances"] + METRICS * len(summary)))


def export_logs(path, scores):
    with open(path, "w") as file:
        for pid, score in scores.items():
            temp = "%d\t%f\t%f\t%f\t%f\n" % (pid, score[0], score[1], score[2], score[3])
            file.write(temp)


if __name__ == '__main__':
    args = CLI.parse_args()

    logging, summary_list = args.log is not None, []

    read_challenge_json(path=args.fold)

    for i in range(len(args.recommendations)):
        s = measure(path1=args.recommendations[i], path2=args.log, k=args.k, term=i)
        summary_list.append(s)

    show_results(summary_list)
