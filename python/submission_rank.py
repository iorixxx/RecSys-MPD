import sys
import csv

HEADER = ["team_info", "Anadolu", "main", "aarslan2@anadolu.edu.tr"]

letor_mapping = {}
prediction_mapping = {}


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


def read_prediction_txt(path):
    line_num = 0

    with open(path, "r") as file:
        for line in file:
            line_num += 1
            prediction_mapping[line_num] = float(line)

    print("Prediction file is read: %s" % path)


def sort_by_predictions(path):
    csv_content = [HEADER]

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

    print("Sorted submission file is created: %s" % path)


if __name__ == '__main__':
    if len(sys.argv) != 4:
        print("Usage: argv0 argv1 argv2 argv3")
        print("argv1: letor txt file")
        print("argv2: prediction txt file")
        print("argv3: submission csv file")
        sys.exit(2)
    else:
        letor_path = sys.argv[1]
        prediction_path = sys.argv[2]
        submission_path = sys.argv[3]

        read_letor_txt(letor_path)
        read_prediction_txt(prediction_path)

        sort_by_predictions(submission_path)

