import csv
import util
import sys

from tabulate import tabulate


CONFIGURATION_KEYS = {"recommendation_csv_list"}


def summarize(path):
    print("\nFile: %s\n" % path)

    pids, count = set(), 0

    with open(path, "r") as file:
        reader = csv.reader(file)
        for row in reader:
            pids.add(int(row[0]))
            count += 1

    print(tabulate([[len(pids), count, count / len(pids)]], headers=["playlists", "recommendations", "avg recommendations"]))


if __name__ == '__main__':
    if len(sys.argv) != 2:
        print("Usage: argv0 argv1")
        print("argv1: Configuration json file")
        sys.exit(2)
    else:
        validation, conf = util.read_configuration_json(sys.argv[1], CONFIGURATION_KEYS)

        if validation:
            for path in conf["recommendation_csv_list"]:
                summarize(path)
        else:
            print("Configuration file cannot be validated, following keys must be satisfied.")
            print(CONFIGURATION_KEYS)
