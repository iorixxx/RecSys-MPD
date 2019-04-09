import argparse

from os.path import join

CLI = argparse.ArgumentParser()

CLI.add_argument("sets", help="Absolute path of the feature sets txt file")
CLI.add_argument("output", help="Absolute path of the output folder")

feature_sets = {}


def load_sets(path):
    with open(path, "r") as f:
        for line in f:
            fields = line.split()
            feature_sets[int(fields[0])] = fields[1]


def export(path):
    for k, v in feature_sets.items():
        with open(join(path, "%d.txt" % k), "w") as out:
            out.write("\n".join(e for e in v.split(",")))


if __name__ == '__main__':
    args = CLI.parse_args()

    load_sets(path=args.sets)
    export(path=args.output)

