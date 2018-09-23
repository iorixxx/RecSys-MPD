import csv
import ast
import sys
import re
import util
import collections


CONFIGURATION_KEYS = {"challenge_json", "track_metadata_csv", "album_metadata_csv", "artist_metadata_csv",
                      "audio_metadata_csv", "recommendation_csv", "output_txt", "features"}

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


challenge_metadata, track_metadata, album_metadata, artist_metadata, audio_metadata, recommendations = {}, {}, {}, {}, {}, {}


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


def read_challenge_json(path):
    challenges = util.read_dataset_json(path)

    for challenge in challenges:
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


def read_recommendation_csv(path):
    with open(path, "r") as file:
        reader = csv.reader(file)
        for row in reader:
            pid = int(row[0])
            track_uri = row[1]
            lucene_score = float(row[2])
            lucene_position = int(row[3])

            if pid not in recommendations:
                recommendations[pid] = collections.OrderedDict()

            recommendations[pid][track_uri] = (lucene_score, lucene_position)

    print("Recommendation file is read: %s" % path)


def convert(path, active_features):
    comments = create_comments(active_features)

    with open(path, "w") as file:
        for c in comments:
            file.write("%s\n" % c)

        for pid, tracks in recommendations.items():
            for fr in collect_features(pid, list(tracks.keys())):
                feature_num = 0
                hit, track_uri, features = fr[0], fr[1], fr[2]

                s = "%d qid:%d" % (hit, pid)

                for f in active_features:
                    feature_num += 1
                    s += " %d:%f" % (feature_num, features[f])

                s += "# %s\n" % track_uri
                file.write(s)

    print("Letor conversion is completed: %s" % path)


def create_comments(active_features):
    comments, feature_num = ["#Extracting features with the following feature vector"], 0

    for f in active_features:
        feature_num += 1
        comments.append("#%d:%s" % (feature_num, FEATURES[f]))

    return comments


def collect_features(pid, track_uris):
    features = []

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

    lucene_score = recommendations[pid][track_uri][0]
    lucene_position = recommendations[pid][track_uri][1]

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

    return hit, track_uri, values


if __name__ == '__main__':
    total_args = len(sys.argv)

    if total_args == 2:
        validation, conf = util.read_configuration_json(sys.argv[1], CONFIGURATION_KEYS)

        if validation:
            conf["features"].sort()

            read_challenge_json(conf["challenge_json"])
            read_track_metadata_csv(conf["track_metadata_csv"])
            read_album_metadata_csv(conf["album_metadata_csv"])
            read_artist_metadata_csv(conf["artist_metadata_csv"])
            read_audio_metadata_csv(conf["audio_metadata_csv"])
            read_recommendation_csv(conf["recommendation_csv"])

            convert(conf["output_txt"], conf["features"])
        else:
            print("Configuration file cannot be validated, following keys must be satisfied.")
            print(CONFIGURATION_KEYS)
    elif total_args == 9:
        read_challenge_json(sys.argv[1])
        read_track_metadata_csv(sys.argv[2])
        read_album_metadata_csv(sys.argv[3])
        read_artist_metadata_csv(sys.argv[4])
        read_audio_metadata_csv(sys.argv[5])
        read_recommendation_csv(sys.argv[6])

        feature_input = ast.literal_eval(sys.argv[7])
        feature_input.sort()

        convert(sys.argv[8], feature_input)
    else:
        print("JSON file based usage: argv0 argv1")
        print("Array based usage: argv0 argv1 argv2 argv3 argv4 argv5 argv6 argv7 argv8")
        sys.exit(2)
