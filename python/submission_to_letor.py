import csv
import json
import sys
import re


CONFIGURATION_KEYS = {"challenge_json", "track_metadata_csv", "album_metadata_csv", "artist_metadata_csv",
                      "audio_metadata_csv", "submission_csv", "lucene_score_csv", "output_txt", "features"}

FEATURES = {1: "Number of tracks in playlist",
            2: "Number of samples in playlist",
            3: "Number of holdouts in playlist",
            4: "Length of playlist title",
            5: "Track occurrence",
            6: "Track frequency",
            7: "Track duration",
            8: "Album occurrence",
            9: "Album frequency",
            10: "Number of tracks in album",
            11: "Artist occurrence",
            12: "Artist frequency",
            13: "Number of albums of artist",
            14: "Number of tracks of artist",
            15: "Submission order",
            16: "Lucene score",
            17: "Prediction position",
            18: "Jaccard distance of playlist title and track name",
            19: "Jaccard distance of playlist title and album name",
            20: "Jaccard distance of playlist title and artist name",
            21: "Audio danceability",
            22: "Audio energy",
            23: "Audio key",
            24: "Audio loudness",
            25: "Audio mode",
            26: "Audio speechiness",
            27: "Audio acousticness",
            28: "Audio instrumentalness",
            29: "Audio liveness",
            30: "Audio valence",
            31: "Audio tempo",
            32: "Audio time signature"}


challenge_metadata, track_metadata, album_metadata, artist_metadata, audio_metadata, lucene_scores = {}, {}, {}, {}, {}, {}


def jaccard_distance(a, b):
    norm_a, norm_b = normalize_name(a), normalize_name(b)
    set_a, set_b = set(norm_a.split()), set(norm_b.split())

    try:
        return float(len(set_a & set_b)) / len(set_a | set_b)
    except ZeroDivisionError:
        return 0


def normalize_name(name):
    name = name.lower()
    name = re.sub(r"[.,/#!$%^*;:{}=_`~()@]", ' ', name)
    name = re.sub(r'\s+', ' ', name).strip()
    return name


def read_configuration_json(path):
    valid = True
    with open(path, "r") as f:
        global conf
        conf = json.load(f)

        if set(conf.keys()) != CONFIGURATION_KEYS or ("features" in conf.keys() and len(conf["features"]) == 0):
            valid = False

    print("Configuration file is read: %s" % path)
    return valid


def read_challenge_json(path):
    with open(path, "r") as f:
        challenges = json.load(f)

        for challenge in challenges["playlists"]:
            pid = challenge["pid"]

            if "name" in challenge.keys():
                name = challenge["name"].strip()
            else:
                name = ""

            num_tracks = challenge["num_tracks"]
            num_samples = challenge["num_samples"]
            num_holdouts = challenge["num_holdouts"]

            holdouts = []
            if "holdouts" in challenge.keys():
                for holdout in challenge["holdouts"]:
                    holdouts.append(holdout["track_uri"])

            tracks = []
            for track in challenge["tracks"]:
                tracks.append(track["track_uri"])

            challenge_metadata[pid] = (name, num_tracks, num_samples, num_holdouts, holdouts, tracks)

    print("Challenge file is read: %s" % path)


def read_track_metadata_csv(path):
    with open(path, "r", encoding="utf-8") as f1:
        reader = csv.reader(f1)
        for row in reader:
            track_uri = row[0]
            occurrence = int(row[1])
            frequency = int(row[2])
            duration = int(row[3])
            album_uri = row[4]
            artist_uri = row[5]
            track_name = row[6]

            track_metadata[track_uri] = (occurrence, frequency, duration, album_uri, artist_uri, track_name)

    print("Track metadata file is read: %s" % path)


def read_album_metadata_csv(path):
    with open(path, "r", encoding="utf-8") as f1:
        reader = csv.reader(f1)
        for row in reader:
            album_uri = row[0]
            occurrence = int(row[1])
            frequency = int(row[2])
            num_tracks_in_album = int(row[3])
            album_name = row[4]

            album_metadata[album_uri] = (occurrence, frequency, num_tracks_in_album, album_name)

    print("Album metadata file is read: %s" % path)


def read_artist_metadata_csv(path):
    with open(path, "r", encoding="utf-8") as f1:
        reader = csv.reader(f1)
        for row in reader:
            artist_uri = row[0]
            occurrence = int(row[1])
            frequency = int(row[2])
            total_albums = int(row[3])
            total_tracks = int(row[4])
            artist_name = row[5]

            artist_metadata[artist_uri] = (occurrence, frequency, total_albums, total_tracks, artist_name)

    print("Artist metadata file is read: %s" % path)


def read_audio_metadata_csv(path):
    with open(path, "r", encoding="utf-8") as f1:
        reader = csv.reader(f1)
        for row in reader:
            try:
                track_uri = row[0]
                danceability = float(row[1])
                energy = float(row[2])
                key = int(row[3])
                loudness = float(row[4])
                mode = int(row[5])
                speechiness = float(row[6])
                acousticness = float(row[7])
                instrumentalness = float(row[8])
                liveness = float(row[9])
                valence = float(row[10])
                tempo = float(row[11])
                time_signature = int(row[12])

                audio_metadata[track_uri] = (danceability, energy, key, loudness, mode, speechiness, acousticness, instrumentalness, liveness, valence, tempo, time_signature)
            except ValueError:
                print("An error occurred for a single row in audio data")

    print("Audio metadata file is read: %s" % path)


def read_lucene_scores_csv(path):
    with open(path, "r") as file:
        reader = csv.reader(file)
        for row in reader:
            if row[0] != "team_info":
                pid = int(row[0])
                scores = [x.strip() for x in row[1:]]

                for s in scores:
                    p1, p2 = s.find("("), s.find(")")
                    track_uri = s[:p1]
                    value = s[p1+1:p2]
                    score = float(value.split("=")[0])
                    position = int(value.split("=")[1])

                    if pid not in lucene_scores:
                        lucene_scores[pid] = {}

                    lucene_scores[pid][track_uri] = (score, position)

    print("Lucene scores file is read: %s" % path)


def process_submission_csv(path1, path2):
    comments = create_comments()

    with open(path1, "r") as f1:
        reader = csv.reader(f1)
        with open(path2, "w") as f2:
            for c in comments:
                f2.write("%s\n" % c)

            for row in reader:
                if row[0] != "team_info":
                    for fr in collect_features(row):
                        feature_num, hit, pid, track_uri, features = 0, fr[0], fr[1], fr[2], fr[3]

                        s = "%d qid:%d" % (hit, pid)

                        for k, v in features.items():
                            if k in conf["features"]:
                                feature_num += 1

                                s += " %d:%f" % (feature_num, v)

                        s += "# %s\n" % track_uri
                        f2.write(s)

    print("Letor conversion is completed: %s" % path2)


def create_comments():
    comments, feature_num = ["#Extracting features with the following feature vector"], 0

    for k, v in FEATURES.items():
        if k in conf["features"]:
            feature_num += 1
            comments.append("#%d:%s" % (feature_num, v))

    return comments


def collect_features(row):
    features = []

    pid = int(row[0])
    track_uris = [x.strip() for x in row[1:]]

    i = len(track_uris)
    for track_uri in track_uris:
        features.append(extract_features(pid, track_uri, i))
        i -= 1

    return features


def extract_features(pid, track_uri, index):
    name = challenge_metadata[pid][0]
    num_tracks = challenge_metadata[pid][1]
    num_samples = challenge_metadata[pid][2]
    num_holdouts = challenge_metadata[pid][3]

    hit = 0
    if track_uri in challenge_metadata[pid][4] or track_uri in challenge_metadata[pid][5]:
        hit = 1

    track_occurrence = track_metadata[track_uri][0]
    track_frequency = track_metadata[track_uri][1]
    track_duration = track_metadata[track_uri][2]
    album_uri = track_metadata[track_uri][3]
    artist_uri = track_metadata[track_uri][4]
    track_name = track_metadata[track_uri][5]

    album_occurrence = album_metadata[album_uri][0]
    album_frequency = album_metadata[album_uri][1]
    num_tracks_in_album = album_metadata[album_uri][2]
    album_name = album_metadata[album_uri][3]

    artist_occurrence = artist_metadata[artist_uri][0]
    artist_frequency = artist_metadata[artist_uri][1]
    total_albums_of_artist = artist_metadata[artist_uri][2]
    total_tracks_of_artist = artist_metadata[artist_uri][3]
    artist_name = artist_metadata[artist_uri][4]

    lucene_score, lucene_position = 0, 0
    if pid in lucene_scores.keys() and track_uri in lucene_scores[pid].keys():
        lucene_score = lucene_scores[pid][track_uri][0]
        lucene_position = lucene_scores[pid][track_uri][1]

    jaccard_track = jaccard_distance(name, track_name)
    jaccard_artist = jaccard_distance(name, artist_name)
    jaccard_album = jaccard_distance(name, album_name)

    danceability, energy, key, loudness, mode, speechiness, acousticness, instrumentalness, liveness, valence, tempo, time_signature = 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0

    if track_uri in audio_metadata.keys():
        danceability = audio_metadata[track_uri][0]
        energy = audio_metadata[track_uri][1]
        key = audio_metadata[track_uri][2]
        loudness = audio_metadata[track_uri][3]
        mode = audio_metadata[track_uri][4]
        speechiness = audio_metadata[track_uri][5]
        acousticness = audio_metadata[track_uri][6]
        instrumentalness = audio_metadata[track_uri][7]
        liveness = audio_metadata[track_uri][8]
        valence = audio_metadata[track_uri][9]
        tempo = audio_metadata[track_uri][10]
        time_signature = audio_metadata[track_uri][11]

    values = {1: num_tracks,
              2: num_samples,
              3: num_holdouts,
              4: len(name),
              5: track_occurrence,
              6: track_frequency,
              7: track_duration,
              8: album_occurrence,
              9: album_frequency,
              10: num_tracks_in_album,
              11: artist_occurrence,
              12: artist_frequency,
              13: total_albums_of_artist,
              14: total_tracks_of_artist,
              15: index,
              16: lucene_score,
              17: lucene_position,
              18: jaccard_track,
              19: jaccard_album,
              20: jaccard_artist,
              21: danceability,
              22: energy,
              23: key,
              24: loudness,
              25: mode,
              26: speechiness,
              27: acousticness,
              28: instrumentalness,
              29: liveness,
              30: valence,
              31: tempo,
              32: time_signature}

    return hit, pid, track_uri, values


if __name__ == '__main__':
    if len(sys.argv) != 2:
        print("Usage: argv0 argv1")
        print("argv1: Configuration json file")
        sys.exit(2)
    else:
        validation = read_configuration_json(sys.argv[1])

        if validation:
            read_challenge_json(conf["challenge_json"])
            read_track_metadata_csv(conf["track_metadata_csv"])
            read_album_metadata_csv(conf["album_metadata_csv"])
            read_artist_metadata_csv(conf["artist_metadata_csv"])
            read_audio_metadata_csv(conf["audio_metadata_csv"])
            read_lucene_scores_csv(conf["lucene_score_csv"])

            process_submission_csv(conf["submission_csv"], conf["output_txt"])
        else:
            print("Configuration file cannot be validated, keys or features may be missing.")
            print(CONFIGURATION_KEYS)
