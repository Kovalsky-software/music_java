package org.example.vp_final;

import javafx.beans.property.*;

public class TrackRow {

    private final IntegerProperty trackId = new SimpleIntegerProperty();
    private final StringProperty title = new SimpleStringProperty();
    private final StringProperty artist = new SimpleStringProperty();
    private final StringProperty album = new SimpleStringProperty();
    private final StringProperty genre = new SimpleStringProperty();
    private final StringProperty duration = new SimpleStringProperty();
    private final StringProperty fileName = new SimpleStringProperty();
    private final BooleanProperty isNew = new SimpleBooleanProperty(false);

    public TrackRow(int trackId, String title, String artist, String album,
                    String genre, String duration, String fileName) {
        this.trackId.set(trackId);
        this.title.set(title != null ? title : "");
        this.artist.set(artist != null ? artist : "");
        this.album.set(album != null ? album : "");
        this.genre.set(genre != null ? genre : "");
        this.duration.set(duration != null ? duration : "");
        this.fileName.set(fileName != null ? fileName : "");
    }

    // === Ð“ÐµÑ‚Ñ‚ÐµÑ€Ñ‹ ÑÐ²Ð¾Ð¹ÑÑ‚Ð² (Ð´Ð»Ñ TableView) ===
    public IntegerProperty trackIdProperty() { return trackId; }
    public StringProperty titleProperty() { return title; }
    public StringProperty artistProperty() { return artist; }
    public StringProperty albumProperty() { return album; }
    public StringProperty genreProperty() { return genre; }
    public StringProperty durationProperty() { return duration; }
    public StringProperty fileNameProperty() { return fileName; }
    public BooleanProperty isNewProperty() { return isNew; }

    // === ÐžÐ±Ñ‹Ñ‡Ð½Ñ‹Ðµ Ð³ÐµÑ‚Ñ‚ÐµÑ€Ñ‹ ===
    public int getTrackId() { return trackId.get(); }
    public String getTitle() { return title.get(); }
    public String getArtist() { return artist.get(); }
    public String getAlbum() { return album.get(); }
    public String getGenre() { return genre.get(); }
    public String getDuration() { return duration.get(); }
    public String getFileName() { return fileName.get(); }
    public boolean isNew() { return isNew.get(); }

    // === Ð¡ÐµÑ‚Ñ‚ÐµÑ€ Ð´Ð»Ñ isNew ===
    public void setNew(boolean value) { this.isNew.set(value); }
}