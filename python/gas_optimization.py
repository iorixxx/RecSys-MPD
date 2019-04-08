import csv
import copy
import argparse

CLI = argparse.ArgumentParser()

CLI.add_argument("weights", help="Absolute path of the weights csv file")
CLI.add_argument("similarities", help="Absolute path of the similarities csv file")
CLI.add_argument("step", help="Step rate", type=float, choices={0.01, 0.05, 0.1})

weights, similarities = {}, {}


def load_weights(path):
    with open(path, "r") as f:
        reader = csv.reader(f)
        for row in reader:
            weights[int(row[0])] = float(row[1])

    print("Weights of features are loaded: %s" % path)


def load_similarities(path):
    with open(path, "r") as f:
        reader = csv.reader(f)
        for row in reader:
            f1 = int(row[0])
            f2 = int(row[1])
            sim = float(row[2])

            if f1 not in similarities:
                similarities[f1] = {}
            similarities[f1][f2] = sim

            if f2 not in similarities:
                similarities[f2] = {}
            similarities[f2][f1] = sim

    print("Similarities of features are loaded: %s" % path)


def punish(f, c, copy_w, copy_s):
    for k in copy_w:
        if k != f:
            copy_w[k] = copy_w[k] - copy_s[k][f] * 2 * c


def drop(f, copy_w, copy_s):
    del copy_w[f]
    del copy_s[f]

    for v in copy_s:
        for k in copy_s[v]:
            if k == f:
                del copy_s[v][k]
                break


def optimize(n, c):
    selected = []
    copy_w = copy.deepcopy(weights)
    copy_s = copy.deepcopy(similarities)

    for i in range(n):
        feature = max(copy_w, key=copy_w.get)

        punish(feature, c, copy_w, copy_s)
        drop(feature, copy_w, copy_s)

        selected.append(feature)

    print("c = %.2f, best %d features: %s" % (c, n, ",".join(str(e) for e in selected)))


if __name__ == '__main__':
    args = CLI.parse_args()

    load_weights(path=args.weights)
    load_similarities(path=args.similarities)

    step, c1 = args.step, 0

    while True:
        c1 = round(c1 + step, 2)
        if c1 > 1:
            break
        else:
            for feat in range(1, len(weights) + 1):
                optimize(feat, c1)
