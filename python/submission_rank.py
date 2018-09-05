import sys
import json
import csv

CONFIGURATION_KEYS = {"letor_txt", "predicted_score_txt", "output_csv"}

letor_mapping, prediction_mapping = {}, {}


def read_configuration_json(path):
    valid = True
    with open(path, "r") as f:
        global conf
        conf = json.load(f)

        if set(conf.keys()) != CONFIGURATION_KEYS:
            valid = False

    print("Configuration file is read: %s" % path)
    return valid


def read_letor_txt(path):
    line_num = 0

    with open(path, "r") as file:
        for line in file:
            if not line.startswith("#"):
                line_num += 1
                tokens = line.split()
                pid = tokens[1].replace("qid:", "")
                track_uri = tokens[-1]

                if pid not in letor_mapping:
                    letor_mapping[pid] = {}

                letor_mapping[pid][track_uri] = line_num

    print("Letor file is read: %s" % path)


def read_predicted_score_txt(path):
    line_num = 0

    with open(path, "r") as file:
        for line in file:
            line_num += 1
            prediction_mapping[line_num] = float(line)

    print("Prediction file is read: %s" % path)


def rank_by_scores(path):
    csv_content = []

    for pid, tracks in letor_mapping.items():
        tuples, submission = [], [pid]

        for track in tracks:
            line_num = tracks[track]
            score = prediction_mapping[line_num]
            tuples.append((track, score))

        tuples.sort(key=lambda tup: tup[1], reverse=True)
        submission.extend(i[0] for i in tuples)
        csv_content.append(submission)

    with open(path, "w", newline='') as f:
        writer = csv.writer(f)
        writer.writerows(csv_content)

    print("Re-ranked result file is created: %s" % path)


if __name__ == '__main__':
    if len(sys.argv) != 2:
        print("Usage: argv0 argv1")
        print("argv1: Configuration json file")
        sys.exit(2)
    else:
        validation = read_configuration_json(sys.argv[1])

        if validation:
            read_letor_txt(conf["letor_txt"])
            read_predicted_score_txt(conf["predicted_score_txt"])

            rank_by_scores(conf["output_csv"])
        else:
            print("Configuration file cannot be validated, keys may be missing.")
            print(CONFIGURATION_KEYS)