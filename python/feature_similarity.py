import csv
import itertools
import statistics
import argparse

from os.path import join

CLI = argparse.ArgumentParser()

CLI.add_argument("recommendations", help="Absolute path of the recommendations folder")
CLI.add_argument("features", help="Number of feature options", type=int)


rankings = {}


def read_recommendations(path, n):
    for i in range(1, n+1):
        rankings[i], rank = {}, 0
        with open(join(path, str(i), "ranked.csv"), "r") as f:
            reader = csv.reader(f)

            for row in reader:
                pid = int(row[0])
                track_uri = row[1]
                rank += 1

                if pid not in rankings[i]:
                    rankings[i][pid] = {}
                rankings[i][pid][track_uri] = rank

    print("Number of ranked csv files read: %d" % len(rankings))


def kendalls_tau(q, vi, vj):
    instances = rankings[vi][q].keys()
    instance_pairs = list(itertools.combinations(instances, 2))

    lst = [x for x in instance_pairs if rankings[vi][q][x[1]] < rankings[vi][q][x[0]] and rankings[vj][q][x[1]] < rankings[vj][q][x[0]]]
    return len(lst) / len(instance_pairs)


def measure(n):
    pairs = list(range(1, n+1))
    feature_pairs = list(itertools.combinations(pairs, 2))

    for fp in feature_pairs:
        pids, lst = rankings[fp[0]].keys(), []

        for pid in pids:
            x = kendalls_tau(q=pid, vi=fp[0], vj=fp[1])
            lst.append(x)

        sim = statistics.mean(lst)
        print("Similarity of %d and %d: %f" % (fp[0], fp[1], sim))


if __name__ == '__main__':
    args = CLI.parse_args()

    read_recommendations(path=args.recommendations, n=args.features)
    measure(n=args.features)
