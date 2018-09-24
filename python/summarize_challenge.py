import argparse
import util
import statistics

from tabulate import tabulate


CLI = argparse.ArgumentParser()

CLI.add_argument("--folds", help="Absolute paths of fold json files to be summarized", nargs="+", required=True)


def summarize(path):
    print("\nFile: %s\n" % path)

    stats, summary = {}, []

    for playlist in util.read_dataset_json(path):
        category = playlist["category"]

        if category not in stats:
            stats[category] = dict(instances=0, num_tracks=[], num_samples=[], num_holdouts=[])

        stats[category]["instances"] += 1
        stats[category]["num_tracks"].append(playlist["num_tracks"])
        stats[category]["num_samples"].append(playlist["num_samples"])
        stats[category]["num_holdouts"].append(playlist["num_holdouts"])

    total, all_tracks, all_samples, all_holdouts = 0, [], [], []

    for k, v in sorted(stats.items()):
        summary.append([k,
                        v["instances"],
                        statistics.mean(v["num_tracks"]),
                        statistics.mean(v["num_samples"]),
                        statistics.mean(v["num_holdouts"])])

        total += v["instances"]

        all_tracks.extend(v["num_tracks"])
        all_samples.extend(v["num_samples"])
        all_holdouts.extend(v["num_holdouts"])

    summary.append(["overall", total, statistics.mean(all_tracks), statistics.mean(all_samples), statistics.mean(all_holdouts)])

    print(tabulate(summary, headers=["category", "instances", "avg tracks", "avg samples", "avg holdouts"]))


if __name__ == '__main__':
    args = CLI.parse_args()

    for f in args.folds:
        summarize(path=f)
