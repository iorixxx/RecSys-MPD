import sys
import util
import csv

CONFIGURATION_KEYS = {"recommendation_csv", "predicted_score_txt", "output_csv"}

letor_mapping, prediction_mapping = {}, {}


def read_recommendation_csv(path):
    line_num = 0
    with open(path, "r") as file:
        reader = csv.reader(file)
        for row in reader:
            line_num += 1
            pid = int(row[0])
            track_uri = row[1]

            if pid not in letor_mapping:
                letor_mapping[pid] = {}
            letor_mapping[pid][track_uri] = line_num

    print("Recommendation file is read: %s" % path)


def read_predicted_score_txt(path):
    line_num = 0
    with open(path, "r") as file:
        for line in file:
            line_num += 1
            prediction_mapping[line_num] = float(line)

    print("Prediction file is read: %s" % path)


def rank_by_scores(path):
    with open(path, "w", newline='') as f:
        writer = csv.writer(f)

        for pid, tracks in letor_mapping.items():
            tuples = []

            for track in tracks:
                line_num = tracks[track]
                score = prediction_mapping[line_num]
                tuples.append((track, score))

            tuples.sort(key=lambda tup: tup[1], reverse=True)

            for t in tuples:
                writer.writerow([pid, t[0], t[1]])

    print("Re-ranked result file is created: %s" % path)


if __name__ == '__main__':
    if len(sys.argv) != 2:
        print("Usage: argv0 argv1")
        print("argv1: Configuration json file")
        sys.exit(2)
    else:
        validation, conf = util.read_configuration_json(sys.argv[1], CONFIGURATION_KEYS)

        if validation:
            read_recommendation_csv(conf["recommendation_csv"])
            read_predicted_score_txt(conf["predicted_score_txt"])

            rank_by_scores(conf["output_csv"])
        else:
            print("Configuration file cannot be validated, following keys must be satisfied.")
            print(CONFIGURATION_KEYS)