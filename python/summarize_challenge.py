import sys
import util
import statistics

from tabulate import tabulate


CONFIGURATION_KEYS = {"challenge_json_list"}


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
    if len(sys.argv) != 2:
        print("Usage: argv0 argv1")
        print("argv1: Configuration json file")
        sys.exit(2)
    else:
        validation, conf = util.read_configuration_json(sys.argv[1], CONFIGURATION_KEYS)

        if validation:
            for path in conf["challenge_json_list"]:
                summarize(path)
        else:
            print("Configuration file cannot be validated, following keys must be satisfied.")
            print(CONFIGURATION_KEYS)
