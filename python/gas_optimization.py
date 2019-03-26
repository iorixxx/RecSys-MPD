import csv
import copy

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

    for v in copy_s.values():
        for k in v.keys():
            if k == f:
                del copy_s[v][k]


def optimize(n, c):
    selected = []
    copy_w = copy.deepcopy(weights)
    copy_s = copy.deepcopy(similarities)

    for i in range(n):
        feature = max(copy_w, key=copy_w.get)

        punish(feature, c, copy_w, copy_s)
        drop(feature, copy_w, copy_s)

        selected.append(feature)

    print()


if __name__ == '__main__':
    print(1)

