import java.nio.file.Path;

public class Track {
    public Path path;
    public String artist;
    public String albumArtist;
    public String album;
    public String title;
    public String year;
    public String genre;

    public Track(Path p, String ar, String alAr, String al, String t, String y, String g) {
        path = p; artist = ar; albumArtist = alAr; album = al; title = t; year = y; genre = g;
    }

    public Path getPath() { return path; }
    public String getArtist() { return artist; }
    public String getAlbumArtist() { return albumArtist; }
    public String getAlbum() { return album; }
    public String getTitle() { return title; }
    public String getYear() { return year; }
    public String getGenre() { return genre; }
}
