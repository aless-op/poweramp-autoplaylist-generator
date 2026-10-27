import java.util.stream.Stream;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Iterator;

import java.io.IOException;
import java.io.BufferedWriter;
import java.io.File;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;

import javax.swing.JFileChooser;

import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.SupportedFileFormat;
import org.jaudiotagger.audio.exceptions.*;
import org.jaudiotagger.tag.TagException;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.TagField;
import org.jaudiotagger.tag.TagTextField;

public class Main {
    public static void main(String[] args) {

        Scanner input = new Scanner(System.in);
        JFileChooser chooser = new JFileChooser();

        Path destinationPath = setDestination(input, chooser);
        if (destinationPath == null) {
            input.close();
            return;
        }

        List<Path> library = setLibraryFolders(input, chooser);
        if (library == null) {
            input.close();
            return;
        }

        Map<String, List<String>> criteria = setCriteria(input);
        if (criteria == null) {
            input.close();
            return;
        }

        input.close();

        List<Track> tracks = scanLibrary(library, criteria);

        List<Track> filteredTracks = filterTracks(tracks, criteria);

        writePlaylist(destinationPath, filteredTracks);
    }

    private static Path setDestination(Scanner input, JFileChooser chooser) {
        // Define filename
        System.out.print("Enter the destination filename: ");
        String filename = input.nextLine() + ".m3u8";

        // Set output location
        System.out.println("Choose the destination folder");
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

        int result = chooser.showOpenDialog(null);
        Path destinationPath;
        if (result == JFileChooser.APPROVE_OPTION) {
            Path destinationFolder = chooser.getSelectedFile().toPath();
            destinationPath = destinationFolder.resolve(filename);
            return destinationPath;
        } else {
            return null;
        }
    }

    private static List<Path> setLibraryFolders(Scanner input, JFileChooser chooser) {
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
                    return null;
                }
            } else {
                System.out.println("Add another folder (1) or continue to scanning (2)?");
                int selection = input.nextInt();
                input.nextLine();
                if (selection == 1) {
                    addAnother = true;
                    int folderSelection = chooser.showOpenDialog(null);
                    if (folderSelection == JFileChooser.APPROVE_OPTION) {
                        Path libraryFolder = chooser.getSelectedFile().toPath();
                        library.add(libraryFolder);
                    } else {
                        return null;
                    }
                } else if (selection == 2) {
                    addAnother = false;
                } else {
                    System.err.println("Invalid selection");
                    addAnother = true;
                }
            }
        } while (addAnother);
        return library;
    }

    private static Map<String, List<String>> setCriteria(Scanner input) {
        Map<String, List<String>> criteria = new HashMap<>();
        boolean addAnother;
        do {
            if (criteria.isEmpty()) {
                addAnother = true;

                String type;
                do {
                    System.out.println("Select a filter type (path, artist, albumartist, album, year, genre):");
                    type = input.nextLine().trim().toLowerCase(Locale.ROOT);
                } while (type.isEmpty());

                List<String> values = criteria.computeIfAbsent(type, k -> new ArrayList<>());
                String newValue;
                boolean valid;
                do {
                    System.out.println("Enter the " + type + " to filter: ");
                    newValue = input.nextLine().trim();
                    if (newValue.isEmpty()) {
                        System.err.println("Value cannot be empty.");
                        valid = false;
                    } else if (values.contains(newValue)) {
                        System.err.println("Value is already saved");
                        valid = false;
                    } else {
                        values.add(newValue);
                        valid = true;
                    }
                } while (!valid);

                List<String> validMethods = List.of("contains", "exact match", "begins with", "ends with");
                do {
                    System.out.println("Enter the matching method:");
                    String method = input.nextLine().trim();
                    if (method.isEmpty()) {
                        System.err.println("Value cannot be empty.");
                        valid = false;
                    } else if (validMethods.contains(method)) {
                        
                    }
                } while (!valid);

            } else {
                System.out.println("Add another criterion (1) or proceed to filtering (2)?");
                int selection = input.nextInt();
                input.nextLine();
                if (selection == 1) {
                    addAnother = true;

                    String type;
                    do {
                        System.out.println("Select a filter type (path, artist, albumartist, album, year, genre):");
                        type = input.nextLine().trim().toLowerCase(Locale.ROOT);
                    } while (type.isEmpty());

                    List<String> values = criteria.computeIfAbsent(type, k -> new ArrayList<>());
                    String newValue;
                    boolean valid;
                    do {
                        System.out.println("Enter the " + type + " to filter: ");
                        newValue = input.nextLine().trim();
                        if (newValue.isEmpty()) {
                            System.err.println("Value cannot be empty.");
                            valid = false;
                        } else if (values.contains(newValue)) {
                            System.err.println("Value is already saved");
                            valid = false;
                        } else {
                            values.add(newValue);
                            valid = true;
                        }
                    } while (!valid);
                } else if (selection == 2) {
                    addAnother = false;
                } else {
                    System.err.println("Invalid selection");
                    addAnother = true;

                }
            }
        } while (addAnother);
        return criteria;
    }

    private static List<Track> scanLibrary(List<Path> library, Map<String, List<String>> criteria) {
        // Create list of Track objects with useful tags from every music file in
        // library
        Set<String> supportedFileFormats = new HashSet<>();
        for (SupportedFileFormat format : SupportedFileFormat.values()) {
            supportedFileFormats.add("." + format.getFilesuffix());
        }
        List<Track> tracks = new ArrayList<>();
        for (int i = 0; i < library.size(); i++) {
            if (Files.isDirectory(library.get(i))) {
                try (Stream<Path> paths = Files.walk(library.get(i))) {
                    Stream<Path> audioPaths = paths
                            .filter(Files::isRegularFile)
                            .filter(path -> supportedFileFormats.stream()
                                    .anyMatch(extension -> path.getFileName().toString()
                                            .toLowerCase(Locale.ROOT)
                                            .endsWith(extension)));
                    Stream<File> audioFiles = audioPaths.map(Path::toFile);
                    audioFiles.forEach(file -> {
                        try {
                            AudioFile f = AudioFileIO.read(file);
                            Tag tag = f.getTag();
                            Map<String, String> tags = new HashMap<>();
                            Iterator<TagField> fields;
                            Track track;
                            if (tag != null) {
                                fields = tag.getFields();
                                while (fields.hasNext()) {
                                    TagField field = fields.next();
                                    if (criteria.containsKey(field.getId().toLowerCase(Locale.ROOT))) {
                                        if (field instanceof TagTextField textField) {
                                            String value = textField.getContent();
                                            tags.put(field.getId().toLowerCase(Locale.ROOT), value);
                                        }
                                    }
                                }
                            }
                            track = new Track(file.toPath(), tags);
                            tracks.add(track);
                        } catch (CannotReadException e) {
                            System.err.println(file + " couldn't be read");
                        } catch (IOException e) {
                            System.err.println(e);
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
        System.out.println("Found " + tracks.size() + " tracks");
        return tracks;
    }

    private static List<Track> filterTracks(List<Track> tracks, Map<String, List<String>> criteria) {
        List<Track> filteredTracks = new ArrayList<>(tracks);
        for (Map.Entry<String, List<String>> criterion : criteria.entrySet()) {
            String type = criterion.getKey();
            List<String> value = criterion.getValue();

            String selectedType = type.toLowerCase(Locale.ROOT);
            System.out.println(type + " - " + value);

            // Filter tracks list
            filteredTracks.removeIf(track -> {
                if (!selectedType.equals("path")) {
                    if (track.getTags().get(selectedType) != null) {
                        return !value.stream().anyMatch(tag -> track.getTags().get(selectedType)
                                .toLowerCase(Locale.ROOT).contains(tag.toLowerCase(Locale.ROOT)));
                    } else {
                        return true;
                    }
                } else {
                    return !value.stream().anyMatch(path -> track.getPath().toString().toLowerCase(Locale.ROOT)
                            .contains(path.toLowerCase(Locale.ROOT)));
                }
            });
        }
        return filteredTracks;
    }

    private static void writePlaylist(Path destinationPath, List<Track> filteredTracks) {
        // From the filtered list, write each track path to a .m3u8 file in the set
        // destination with the set filename
        try (BufferedWriter fileWriter = Files.newBufferedWriter(destinationPath, StandardCharsets.UTF_8)) {
            for (Track track : filteredTracks) {
                fileWriter.write(track.getPath().toString() + "\r\n");
            }
            System.out.println("Playlist created with " + filteredTracks.size() + " tracks");
        } catch (IOException e) {
            System.err.println("An error occured");
            e.printStackTrace();
        }
    }
}
