import csv
import re
import util
import collections
import math
import argparse

CLI = argparse.ArgumentParser()

CLI.add_argument("fold", help="Absolute path of the fold json file")
CLI.add_argument("recommendation", help="Absolute path of the recommendation csv file")
CLI.add_argument("output", help="Absolute path of the output txt file")
CLI.add_argument("trackMeta", help="Absolute path of the track metadata csv file")
CLI.add_argument("albumMeta", help="Absolute path of the album metadata csv file")
CLI.add_argument("artistMeta", help="Absolute path of the artist metadata csv file")
CLI.add_argument("audioMeta", help="Absolute path of the audio metadata csv file")
CLI.add_argument("--features", help="Features to be included in letor conversion", nargs="+", type=int, required=True)


FEATURES = {1: "Number of samples in playlist",
            2: "Length of playlist title",
            3: "Track occurrence",
            4: "Track frequency",
            5: "Track duration",
            6: "Album occurrence",
            7: "Album frequency",
            8: "Number of tracks in album",
            9: "Artist occurrence",
            10: "Artist frequency",
            11: "Number of albums of artist",
            12: "Number of tracks of artist",
            13: "Jaccard distance of playlist title and track name",
            14: "Jaccard distance of playlist title and album name",
            15: "Jaccard distance of playlist title and artist name",
            16: "Audio danceability",
            17: "Audio energy",
            18: "Audio key",
            19: "Audio loudness",
            20: "Audio mode",
            21: "Audio speechiness",
            22: "Audio acousticness",
            23: "Audio instrumentalness",
            24: "Audio liveness",
            25: "Audio valence",
            26: "Audio tempo",
            27: "Audio time signature",
            28: "Submission order",
            29: "Position",
            30: "Max Lucene score",
            31: "Track search result frequency",
            32: "Album search result frequency",
            33: "Artist search result frequency",
            34: "Geometric mean of track search result frequency and max Lucene score",
            35: "Geometric mean of album search result frequency and max Lucene score",
            36: "Geometric mean of artist search result frequency and max Lucene score",
            37: "Album density",
            38: "Artist density"}


recommendations = collections.OrderedDict()
challenge_metadata, track_metadata, album_metadata, artist_metadata, audio_metadata = {}, {}, {}, {}, {}


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

        num_samples = challenge["num_samples"]

        if "name" in challenge.keys():
            name = challenge["name"].strip()
        else:
            name = ""

        holdouts = []
        for holdout in challenge["holdouts"]:
            holdouts.append(holdout["track_uri"])

        tracks, albums, artists = [], [], []
        for track in challenge["tracks"]:
            tracks.append(track["track_uri"])
            albums.append(track["album_uri"])
            artists.append(track["artist_uri"])

        challenge_metadata[pid] = (num_samples, name, holdouts, tracks, albums, artists)

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
            track_srf = int(row[2])
            max_lucene_score = float(row[3])
            position = int(row[4])
            album_srf = int(row[5])
            artist_srf = int(row[6])

            if pid not in recommendations:
                recommendations[pid] = collections.OrderedDict()

            recommendations[pid][track_uri] = (track_srf, max_lucene_score, position, album_srf, artist_srf)

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


def extract_features(pid, track_uri, order):
    num_samples = challenge_metadata[pid][0]
    name = challenge_metadata[pid][1]
    holdouts = challenge_metadata[pid][2]
    seed_tracks = challenge_metadata[pid][3]
    seed_albums = challenge_metadata[pid][4]
    seed_artists = challenge_metadata[pid][5]

    hit = 0
    if track_uri in holdouts or track_uri in seed_tracks:
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

    jaccard_track = jaccard_distance(name, track_name)
    jaccard_artist = jaccard_distance(name, artist_name)
    jaccard_album = jaccard_distance(name, album_name)

    track_srf = recommendations[pid][track_uri][0]
    lucene_score = recommendations[pid][track_uri][1]
    position = recommendations[pid][track_uri][2]
    album_srf = recommendations[pid][track_uri][3]
    artist_srf = recommendations[pid][track_uri][4]

    geo_track = math.sqrt(track_srf * lucene_score)
    geo_album = math.sqrt(album_srf * lucene_score)
    geo_artist = math.sqrt(artist_srf * lucene_score)

    album_density = seed_albums.count(album_uri) / len(seed_albums)
    artist_density = seed_artists.count(artist_uri) / len(seed_artists)

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

    values = {1: num_samples,
              2: len(name),
              3: track_occurrence,
              4: track_frequency,
              5: track_duration,
              6: album_occurrence,
              7: album_frequency,
              8: num_tracks_in_album,
              9: artist_occurrence,
              10: artist_frequency,
              11: total_albums_of_artist,
              12: total_tracks_of_artist,
              13: jaccard_track,
              14: jaccard_album,
              15: jaccard_artist,
              16: danceability,
              17: energy,
              18: key,
              19: loudness,
              20: mode,
              21: speechiness,
              22: acousticness,
              23: instrumentalness,
              24: liveness,
              25: valence,
              26: tempo,
              27: time_signature,
              28: order,
              29: position,
              30: lucene_score,
              31: track_srf,
              32: album_srf,
              33: artist_srf,
              34: geo_track,
              35: geo_album,
              36: geo_artist,
              37: album_density,
              38: artist_density}

    return hit, track_uri, values


if __name__ == '__main__':
    args = CLI.parse_args()

    read_challenge_json(path=args.fold)
    read_track_metadata_csv(path=args.trackMeta)
    read_album_metadata_csv(path=args.albumMeta)
    read_artist_metadata_csv(path=args.artistMeta)
    read_audio_metadata_csv(path=args.audioMeta)
    read_recommendation_csv(path=args.recommendation)

    convert(path=args.output, active_features=sorted(args.features))
