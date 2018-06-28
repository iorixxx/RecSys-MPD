import sys
import csv
import json
import statistics

recommendations, challenge_set = {}, {}


def evaluate(holdout_tracks, recommended_tracks):
    precision_hits, recall_hits = 0, 0

    for i in range(len(recommended_tracks)):
        track_uri = recommended_tracks[i]

        if track_uri in holdout_tracks:
            recall_hits += 1
            if i < len(holdout_tracks):
                precision_hits += 1

    return [precision_hits / len(holdout_tracks), recall_hits / len(holdout_tracks)]


def read_recommendations_csv(path):
    with open(path, "r") as f:
        reader = csv.reader(f)
        for row in reader:
            if row[0] != "team_info":
                pid = int(row[0])
                recommendations[pid] = []
                for item in row[1:]:
                    recommendations[pid].append(item.strip())

    print("Recommendation file is read")


def read_challenge_set_json(path):
    with open(path, "r") as f:
        data_holder = json.load(f)

        for item in data_holder["playlists"]:
            pid = item["pid"]
            category = int(item["category"].replace("cat", ""))
            holdouts = []

            for h in item["holdouts"]:
                holdouts.append(h["track_uri"])

            challenge_set[pid] = dict(category=category, holdouts=holdouts)

    print("Challenge file is read")


def measure():
    avg_num_holdouts, precision, recall = [], [], []
    categorical_avg_num_holdouts, categorical_precision, categorical_recall = {}, {}, {}

    for k, v in recommendations.items():
        cat = challenge_set[k]["category"]
        num_holdouts = len(challenge_set[k]["holdouts"])
        scores = evaluate(challenge_set[k]["holdouts"], v)

        if cat not in categorical_precision:
            categorical_avg_num_holdouts[cat] = []
            categorical_precision[cat] = []
            categorical_recall[cat] = []

        avg_num_holdouts.append(num_holdouts)
        precision.append(scores[0])
        recall.append(scores[1])

        categorical_avg_num_holdouts[cat].append(num_holdouts)
        categorical_precision[cat].append(scores[0])
        categorical_recall[cat].append(scores[1])

    for cat in sorted(categorical_precision.keys()):
        anh = statistics.mean(categorical_avg_num_holdouts[cat])
        trp = statistics.mean(categorical_precision[cat])
        arp = statistics.mean(categorical_recall[cat])
        print("%s\t%f\t%f\t%f" % (cat, anh, trp, arp))

    print("%s\t%f\t%f\t%f\t" % ("mean", statistics.mean(avg_num_holdouts), statistics.mean(precision), statistics.mean(recall)))


if __name__ == '__main__':
    if len(sys.argv) != 3:
        print("Usage: argv0 argv1 argv2")
        print("argv1: recommendations csv file")
        print("argv2: challenge set json file")
        sys.exit(2)
    else:
        recommendations_path = sys.argv[1]
        challenge_set_path = sys.argv[2]

        read_recommendations_csv(recommendations_path)
        read_challenge_set_json(challenge_set_path)

        measure()


