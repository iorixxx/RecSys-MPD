import csv
import json
import sys
import re

USE_CREATIVE_FEATURES = False

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


def read_album_popularities_csv(path):
    album_popularities = {}

    with open(path, "r", encoding="utf-8") as f1:
        reader = csv.reader(f1)
        for row in reader:
            album_popularities[row[0]] = int(row[1])

    print("Album popularity file is read: %s" % path)
    return album_popularities


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


def read_album_metadata_csv(path1, path2):
    album_popularities = read_album_popularities_csv(path2)

    with open(path1, "r", encoding="utf-8") as f1:
        reader = csv.reader(f1)
        for row in reader:
            album_uri = row[0]
            occurrence = int(row[1])
            frequency = int(row[2])
            num_tracks_in_album = int(row[3])
            album_name = row[4]

            popularity = 0
            if album_uri in album_popularities.keys():
                popularity = album_popularities[album_uri]

            album_metadata[album_uri] = (occurrence, frequency, num_tracks_in_album, album_name, popularity)

    print("Album metadata file is read: %s" % path1)


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
                        if USE_CREATIVE_FEATURES:
                            f2.write("%d qid:%d 1:%f 2:%f 3:%f 4:%f 5:%f 6:%f 7:%f 8:%f 9:%f 10:%f 11:%f 12:%f 13:%f 14:%f 15:%f 16:%f 17:%f 18:%f 19:%f 20:%f 21:%f 22:%f 23:%f 24:%f 25:%f 26:%f 27:%f 28:%f 29:%f 30:%f 31:%f 32:%f 33:%f # %s\n" % tuple(fr))
                        else:
                            f2.write("%d qid:%d 1:%f 2:%f 3:%f 4:%f 5:%f 6:%f 7:%f 8:%f 9:%f 10:%f 11:%f 12:%f 13:%f 14:%f 15:%f 16:%f 17:%f 18:%f 19:%f 20:%f # %s\n" % tuple(fr))

    print("Letor conversion is completed: %s" % path2)


def create_comments():
    if USE_CREATIVE_FEATURES:
        return ["#Extracting features with the following feature vector",
                "#1:Number of tracks in playlist",
                "#2:Number of samples in playlist",
                "#3:Number of holdouts in playlist",
                "#4:Length of playlist title",
                "#5:Track occurrence",
                "#6:Track frequency",
                "#7:Track duration",
                "#8:Album occurrence",
                "#9:Album frequency",
                "#10:Number of tracks in album",
                "#11:Artist occurrence",
                "#12:Artist frequency",
                "#13:Number of albums of artist",
                "#14:Number of tracks of artist",
                "#15:Submission order",
                "#16:Lucene score",
                "#17:Prediction position",
                "#18:Jaccard distance of playlist title and track name",
                "#19:Jaccard distance of playlist title and album name",
                "#20:Jaccard distance of playlist title and artist name",
                "#21:Audio danceability",
                "#22:Audio energy",
                "#23:Audio key",
                "#24:Audio loudness",
                "#25:Audio mode",
                "#26:Audio speechiness",
                "#27:Audio acousticness",
                "#28:Audio instrumentalness",
                "#29:Audio liveness",
                "#30:Audio valence",
                "#31:Audio tempo",
                "#32:Audio time signature",
                "#33:Album popularity"]
    else:
        return ["#Extracting features with the following feature vector",
                "#1:Number of tracks in playlist",
                "#2:Number of samples in playlist",
                "#3:Number of holdouts in playlist",
                "#4:Length of playlist title",
                "#5:Track occurrence",
                "#6:Track frequency",
                "#7:Track duration",
                "#8:Album occurrence",
                "#9:Album frequency",
                "#10:Number of tracks in album",
                "#11:Artist occurrence",
                "#12:Artist frequency",
                "#13:Number of albums of artist",
                "#14:Number of tracks of artist",
                "#15:Submission order",
                "#16:Lucene score",
                "#17:Prediction position"
                "#18:Jaccard distance of playlist title and track name",
                "#19:Jaccard distance of playlist title and album name",
                "#20:Jaccard distance of playlist title and artist name"]


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
    album_popularity = album_metadata[album_uri][4]

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

    values = [hit, pid, num_tracks, num_samples, num_holdouts, len(name),
              track_occurrence, track_frequency, track_duration, album_occurrence, album_frequency, num_tracks_in_album,
              artist_occurrence, artist_frequency, total_albums_of_artist, total_tracks_of_artist, index, lucene_score, lucene_position,
              jaccard_track, jaccard_album, jaccard_artist]

    if USE_CREATIVE_FEATURES:
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

        values.extend([danceability, energy, key, loudness, mode, speechiness, acousticness, instrumentalness, liveness, valence, tempo, time_signature, album_popularity])

    values.append(track_uri)

    return values


if __name__ == '__main__':
    if len(sys.argv) != 11:
        print("Usage: argv0 argv1 argv2 argv3 argv4 argv5 argv6 argv7 argv8 argv9 argv10")
        print("argv1: challenge test set json file")
        print("argv2: track metadata csv file")
        print("argv3: album metadata csv file")
        print("argv4: artist metadata csv file")
        print("argv5: audio metadata csv file")
        print("argv6: album popularity csv file")
        print("argv7: submission csv file")
        print("argv8: lucene score csv file")
        print("argv9: letor output txt file")
        print("argv10: use creative features (True or False)")
        sys.exit(2)
    else:
        challenge_path = sys.argv[1]
        track_metadata_path = sys.argv[2]
        album_metadata_path = sys.argv[3]
        artist_metadata_path = sys.argv[4]
        audio_metadata_path = sys.argv[5]
        album_popularity_path = sys.argv[6]
        submission_path = sys.argv[7]
        lucene_score_path = sys.argv[8]
        letor_path = sys.argv[9]

        if sys.argv[10] == "True":
            USE_CREATIVE_FEATURES = True

        read_challenge_json(challenge_path)
        read_track_metadata_csv(track_metadata_path)
        read_album_metadata_csv(album_metadata_path, album_popularity_path)
        read_artist_metadata_csv(artist_metadata_path)
        read_lucene_scores_csv(lucene_score_path)

        if USE_CREATIVE_FEATURES:
            read_audio_metadata_csv(audio_metadata_path)

        process_submission_csv(submission_path, letor_path)