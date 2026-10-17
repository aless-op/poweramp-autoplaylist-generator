import java.util.stream.Stream;
import java.io.IOException;
import java.nio.file.Path;
import java.io.File;
import java.nio.file.Files;

import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.exceptions.*;
import org.jaudiotagger.tag.TagException;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;

public class Main {
    public static void main(String[] args){
        Path musicPath = Path.of(System.getProperty("user.home")).resolve("Music");
        Path[] library = {musicPath.resolve("Artists"), musicPath.resolve("Soundtracks")};
        for (int i = 0; i < library.length; i++) {
            if (Files.isDirectory(library[i])) {
                try (Stream<Path> paths = Files.walk(library[i])) {
                    Stream<Path> filePaths = paths.filter(Files::isRegularFile);
                    Stream<Path> flacPaths = filePaths.filter(path -> path.toString().endsWith(".flac"));
                    Stream<File> flacFiles = flacPaths.map(Path::toFile);
                    flacFiles.forEach(file -> {try {
                        AudioFile f = AudioFileIO.read(file);
                        Tag tag = f.getTag();
                        Track track = new Track(
                            file.toPath(),
                            tag.getFirst(FieldKey.ARTIST).toString(),
                            tag.getFirst(FieldKey.ALBUM_ARTIST).toString(),
                            tag.getFirst(FieldKey.ALBUM).toString(),
                            tag.getFirst(FieldKey.TITLE).toString(),
                            tag.getFirst(FieldKey.YEAR),
                            tag.getFirst(FieldKey.GENRE).toString()
                        );
                    } catch (CannotReadException e) {
                        System.out.println(file + " couldn't be read");
                    } catch (IOException e) {
                        System.out.println("I/O exception");
                    } catch (TagException e) {
                        System.out.println(e);
                    } catch (ReadOnlyFileException e) {
                        System.out.println(file + " is a read only file");
                    } catch (InvalidAudioFrameException e) {
                        System.out.println(e);
                    }
                    });
                } catch (IOException e) {
                    System.out.println("Error reading directory");
                }
            } else {
                System.out.println(library[i] + " is not a valid path");
            }
        }
    }
}
