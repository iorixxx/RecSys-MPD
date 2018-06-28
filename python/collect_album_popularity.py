import sys
import csv
import spotipy
from spotipy.oauth2 import SpotifyClientCredentials

MAX_BATCH = 20


def extract_albums():
    albums = []

    with open(ALBUM_METADATA_PATH, "r", encoding="utf-8") as f1:
        reader = csv.reader(f1)
        for row in reader:
            albums.append(row[0])

    print("Album metadata file is read: %s" % ALBUM_METADATA_PATH)
    return albums


def count_batches(t):
    length_t = len(t)

    if length_t % MAX_BATCH == 0:
        return int(length_t / MAX_BATCH)
    else:
        return int(length_t / MAX_BATCH) + 1


def collect():
    client_credentials_manager = SpotifyClientCredentials(client_id=CLIENT_ID, client_secret=CLIENT_SECRET)
    sp = spotipy.Spotify(client_credentials_manager=client_credentials_manager)

    albums = extract_albums()
    batches = count_batches(albums)

    with open(ALBUM_POPULARITY_PATH, "w", newline='') as f:
        writer = csv.writer(f)
        for i in range(batches):
            print("Batch %d" % i)
            sub_list = albums[i*MAX_BATCH : (i+1)*MAX_BATCH]
            pops = sp.albums(sub_list)

            for pop in pops["albums"]:
                if pop is not None:
                    writer.writerow([pop["uri"], pop["popularity"]])

    print("Album popularities are collected: %s" % ALBUM_POPULARITY_PATH)


if __name__ == '__main__':
    if len(sys.argv) != 5:
        print("Usage: argv0 argv1 argv2 argv3 argv4")
        print("argv1: album metadata csv file")
        print("argv2: album popularity output csv file")
        print("argv3: Client ID")
        print("argv4: Client Secret")
        sys.exit(2)
    else:
        ALBUM_METADATA_PATH = sys.argv[1]
        ALBUM_POPULARITY_PATH = sys.argv[2]
        CLIENT_ID = sys.argv[3]
        CLIENT_SECRET = sys.argv[4]

        collect()