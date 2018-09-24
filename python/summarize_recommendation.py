import csv
import argparse

from tabulate import tabulate

CLI = argparse.ArgumentParser()

CLI.add_argument("--recommendations", help="Absolute paths of recommendation csv files to be summarized", nargs="+", required=True)


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
    args = CLI.parse_args()

    for r in args.recommendations:
        summarize(path=r)
