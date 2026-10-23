import java.nio.file.Path;

import java.util.Map;

public class Track {
    private Path path;
    private Map<String, String> tags;

    public Track(Path p, Map<String, String> t) {
        this.path = p;
        this.tags = t;
    }

    public Path getPath() { return path; }
    public Map<String, String> getTags() {return tags;}
}
