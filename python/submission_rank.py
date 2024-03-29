import csv
import argparse


CLI = argparse.ArgumentParser()

CLI.add_argument("output", help="Absolute path of the output csv file")
CLI.add_argument("recommendation", help="Absolute path of the recommendation csv file")
CLI.add_argument("prediction", help="Absolute path of the prediction txt file")
CLI.add_argument("library", help="Library that generates the prediction txt file")

SUPPORTED_LIBRARIES = ["jforests", "ranklib"]

letor_mapping, prediction_mapping = {}, {}


def read_recommendations(path):
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


def read_predictions(path, library):
    switcher = {SUPPORTED_LIBRARIES[0]: lambda x: float(x),
                SUPPORTED_LIBRARIES[1]: lambda x: float(x.split("\t")[2])}

    line_num = 0
    with open(path, "r") as file:
        for line in file:
            line_num += 1

            func = switcher.get(library)
            value = func(line)

            prediction_mapping[line_num] = value

    print("Prediction file is read: %s" % path)


def rank(path):
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
    args = CLI.parse_args()

    if args.library not in SUPPORTED_LIBRARIES:
        raise SystemExit("LTR library must be jforests or ranklib")

    read_recommendations(path=args.recommendation)
    read_predictions(path=args.prediction, library=args.library)
    rank(path=args.output)
