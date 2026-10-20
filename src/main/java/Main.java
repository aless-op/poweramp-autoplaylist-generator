import java.lang.reflect.Field;

import java.util.stream.Stream;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.Map;
import java.util.HashMap;
import java.util.Locale;
import java.util.Arrays;

import java.io.IOException;
import java.io.BufferedWriter;
import java.io.File;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;

import javax.swing.JFileChooser;

import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.exceptions.*;
import org.jaudiotagger.tag.TagException;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;

public class Main {
    public static void main(String[] args) {

        // Define filename
        Scanner input = new Scanner(System.in);
        System.out.print("Enter the destination filename: ");
        String filename = input.nextLine() + ".m3u8";

        // Set output location
        JFileChooser chooser = new JFileChooser();
        System.out.println("Choose the destination folder");
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

        int result = chooser.showOpenDialog(null);
        Path destinationPath;
        if (result == JFileChooser.APPROVE_OPTION) {
            Path destinationFolder = chooser.getSelectedFile().toPath();
            destinationPath = destinationFolder.resolve(filename);
        } else {
            input.close();
            return;
        }

        // Select music library folders - TODO: Ask to repeat selection to add multiple locations
        List<Path> library = new ArrayList<>();
        boolean addAnother;
        do {
            if (library.size() == 0) {
                addAnother = true;
                System.out.println("Add a folder containing music files");
                int folderSelection = chooser.showOpenDialog(null);
                if (folderSelection == JFileChooser.APPROVE_OPTION) {
                    Path libraryFolder = chooser.getSelectedFile().toPath();
                    library.add(libraryFolder);
                } else {
                    input.close();
                    return;
                }
            } else {
                System.out.println("Add another folder (1) or continue to scanning (2)");
                int selection = input.nextInt();
                input.nextLine();
                if (selection == 1) {
                    addAnother = true;
                    int folderSelection = chooser.showOpenDialog(null);
                    if (folderSelection == JFileChooser.APPROVE_OPTION) {
                        Path libraryFolder = chooser.getSelectedFile().toPath();
                        library.add(libraryFolder);
                    } else {
                        input.close();
                        return;
                    }
                } else if (selection == 2) {
                    addAnother = false;
                } else {
                    System.err.println("Invalid selection");
                    addAnother = true;
                }
            }
        } while (addAnother);
            


        // Create list of Track objects with useful tags from every flac file in library - TODO: Allow different file formats (filter by format?)
        List<Track> tracks = new ArrayList<>();
        for (int i = 0; i < library.size(); i++) {
            if (Files.isDirectory(library.get(i))) {
                try (Stream<Path> paths = Files.walk(library.get(i))) {
                    Stream<Path> filePaths = paths.filter(Files::isRegularFile);
                    Stream<Path> flacPaths = filePaths.filter(path -> path.toString().endsWith(".flac"));
                    Stream<File> flacFiles = flacPaths.map(Path::toFile);
                    flacFiles.forEach(file -> {
                        try {
                            AudioFile f = AudioFileIO.read(file);
                            Tag tag = f.getTag();
                            Track track = new Track(
                                    file.toPath(),
                                    tag.getFirst(FieldKey.ARTIST).toString(),
                                    tag.getFirst(FieldKey.ALBUM_ARTIST).toString(),
                                    tag.getFirst(FieldKey.ALBUM).toString(),
                                    tag.getFirst(FieldKey.TITLE).toString(),
                                    tag.getFirst(FieldKey.YEAR).toString(),
                                    tag.getFirst(FieldKey.GENRE).toString());
                            tracks.add(track);
                        } catch (CannotReadException e) {
                            System.err.println(file + " couldn't be read");
                        } catch (IOException e) {
                            System.err.println("I/O exception");
                        } catch (TagException e) {
                            System.err.println(e);
                        } catch (ReadOnlyFileException e) {
                            System.err.println(file + " is a read only file");
                        } catch (InvalidAudioFrameException e) {
                            System.err.println(e);
                        }
                    });
                } catch (IOException e) {
                    System.err.println("Error reading directory");
                }
            } else {
                System.out.println(library.get(i) + " is not a valid path");
            }
        }

        // Start filtering: ask for attribute to filter and its value - TODO: Allow criteria combinations
        Map<String, String> criteria = new HashMap<>();
        System.out.println("Select a filter type (path, artist, albumartist, album, year, genre):");
        String type = input.nextLine().trim().toLowerCase(Locale.ROOT);
        System.out.println("Enter the " + type + " to filer: ");
        String value = input.nextLine().trim();
        criteria.put(type, value);
        String selectedType = type.replace(" ", "").toLowerCase(Locale.ROOT);
        System.out.println(type + " - " + criteria.get(type));
        input.close();

        Field field = Arrays.stream(Track.class.getDeclaredFields())
            .filter(f -> f.getName().toLowerCase(Locale.ROOT).equals(selectedType))
            .findFirst()
            .orElseThrow();


        // Filter tracks list based on the criteria just specified
        tracks.removeIf(track -> {
            try {
                return !field.get(track).toString().toLowerCase(Locale.ROOT).contains(criteria.get(selectedType).toLowerCase(Locale.ROOT));
            } catch (IllegalAccessException e) {
                // TODO: handle exception
                return false;
            }
        });

        // From the filtered list, write each track path to a .m3u8 file in the set destination with the set filename
        try (BufferedWriter fileWriter = Files.newBufferedWriter(destinationPath, StandardCharsets.UTF_8)) {
            for (Track track : tracks) {
                fileWriter.write(track.getPath().toString() + "\r\n");
            }
        } catch (IOException e) {
            System.err.println("An error occured");
            e.printStackTrace();
        }
        System.out.println("Playlist created");
    }

    
}
