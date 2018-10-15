package edu.anadolu;

public enum SearchFieldConfig {

    Track,
    Album,
    Artist;

    public String getSearchField() {
        switch (this) {
            case Track:
                return "track_uris";

            case Album:
                return "album_uris";

            case Artist:
                return "artist_uris";

            default:
                throw new AssertionError(this);
        }
    }
}
