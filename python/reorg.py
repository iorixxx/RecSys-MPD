import csv
import sys
import collections


dictio = collections.OrderedDict()


def read(file):
    with open(file, "r") as f1:
        reader = csv.reader(f1)

        for row in reader:
            pid = int(row[0])
            track_uri = row[1]
            frequency = int(row[2])
            score = float(row[3])
            pos = int(row[4])

            if pid not in dictio:
                dictio[pid] = []

            dictio[pid].append((track_uri, frequency, score, pos))


def overwrite(file):
    with open(file, "w", newline='') as f1:
        writer = csv.writer(f1)

        for pid, tuples in dictio.items():
            for t in tuples:
                writer.writerow([pid, t[0], t[1], t[2], t[3]])


if __name__ == '__main__':
    if len(sys.argv) != 2:
        print("2 args")
    else:
        f = sys.argv[1]
        read(f)
        overwrite(f)