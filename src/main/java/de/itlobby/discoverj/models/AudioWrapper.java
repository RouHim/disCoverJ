package de.itlobby.discoverj.models;

import de.itlobby.discoverj.util.AudioUtil;
import de.itlobby.discoverj.util.StringUtil;
import javafx.scene.image.Image;
import org.apache.commons.lang3.ObjectUtils;
import org.jaudiotagger.audio.AudioFile;

import java.io.File;
import java.util.Objects;
import java.util.Optional;

public class AudioWrapper implements Comparable<AudioWrapper> {

    private final Integer id;
    private final String filePath;
    private final String artist;
    private final String title;
    private final String album;
    private final String trackNumber;
    private final String parentFilePath;
    private final String fileName;
    private final long fileLength;
    private final boolean readOnly;
    private final String fileNameExtension;
    private final String year;
    private final String albumArtist;
    private boolean hasCover;

    public AudioWrapper(
            Integer id,
            String filePath,
            boolean hasCover,
            String artist,
            String title,
            String album,
            String trackNumber,
            String parentFilePath,
            String fileName,
            long fileLength,
            boolean readOnly,
            String fileNameExtension,
            String year,
            String albumArtist
    ) {
        this.id = id;
        this.filePath = filePath;
        this.hasCover = hasCover;
        this.artist = artist;
        this.title = title;
        this.album = album;
        this.trackNumber = trackNumber;
        this.parentFilePath = parentFilePath;
        this.fileName = fileName;
        this.fileLength = fileLength;
        this.readOnly = readOnly;
        this.fileNameExtension = fileNameExtension;
        this.year = year;
        this.albumArtist = albumArtist;
    }

    public AudioWrapper(Integer id, AudioFile audioFile) {
        this.id = id;
        this.filePath = audioFile.getFile().getAbsolutePath();
        this.fileName = audioFile.getFile().getName();
        this.parentFilePath = audioFile.getFile().getParentFile().getAbsolutePath();
        this.hasCover = AudioUtil.haveCover(audioFile);
        this.artist = AudioUtil.getArtist(audioFile);
        this.title = AudioUtil.getTitle(audioFile);
        this.album = AudioUtil.getAlbum(audioFile);
        this.year = AudioUtil.getYear(audioFile);
        this.albumArtist = AudioUtil.getAlbumArtist(audioFile);
        this.trackNumber = AudioUtil.getTrackNumber(audioFile);
        this.readOnly = !audioFile.getFile().canRead();
        this.fileLength = audioFile.getFile().length();
        this.fileNameExtension = StringUtil.getFileExtension(fileName);
    }

    public Integer getId() {
        return id;
    }

    public String getFilePath() {
        return filePath;
    }

    public boolean hasCover() {
        return hasCover;
    }

    public String getArtist() {
        return artist;
    }

    public String getTitle() {
        return title;
    }

    public String getAlbum() {
        return album;
    }

    public String getTrackNumber() {
        return trackNumber;
    }

    public String getParentFilePath() {
        return parentFilePath;
    }

    public String getFileName() {
        return fileName;
    }

    public boolean isReadOnly() {
        return readOnly;
    }

    public boolean canWrite() {
        return !readOnly;
    }

    public long getFileLength() {
        return fileLength;
    }

    public String getFileNameExtension() {
        return fileNameExtension;
    }

    public String getYear() {
        return year;
    }

    public void setHasCover(boolean hasCover) {
        this.hasCover = hasCover;
    }

    public String getAlbumArtist() {
        return albumArtist;
    }

    public String getDisplayValue() {
        String res = "";

        if (!StringUtil.isNullOrEmpty(this.getArtist())) {
            res += this.getArtist();

            if (!StringUtil.isNullOrEmpty(this.getTitle())) {
                res += " - " + this.getTitle();
            }
        } else {
            res += this.getFileName();
        }

        if (!StringUtil.isNullOrEmpty(this.getAlbum())) {
            res += "\n" + this.getAlbum();
        }

        return res;
    }

    @Override
    public int compareTo(AudioWrapper other) {
        if (other == null) {
            return 1;
        }

        String thisFolder = this.getParentFilePath();
        String otherFolder = other.getParentFilePath();

        int folderComparisonResult = compareFolders(thisFolder, otherFolder);
        if (folderComparisonResult != 0) {
            return folderComparisonResult;
        }

        int albumComparisonResult = compareAlbums(this.album, other.album);
        if (albumComparisonResult != 0) {
            return albumComparisonResult;
        }

        int trackNumberComparisonResult = compareTrackNumbers(this.trackNumber, other.trackNumber);
        if (trackNumberComparisonResult != 0) {
            return trackNumberComparisonResult;
        }

        return compareFileNames(this.filePath, other.filePath);
    }

    private int compareFolders(String thisFolder, String otherFolder) {
        return thisFolder.compareTo(otherFolder);
    }

    private int compareAlbums(String thisAlbum, String otherAlbum) {
        return ObjectUtils.compare(thisAlbum, otherAlbum);
    }

    private int compareTrackNumbers(String thisTrackNumber, String otherTrackNumber) {
        if (isEmpty(thisTrackNumber) && isEmpty(otherTrackNumber)) {
            return 0;
        }

        if (isInteger(thisTrackNumber) && isInteger(otherTrackNumber)) {
            int thisTrackNumberInt = Integer.parseInt(thisTrackNumber);
            int otherTrackNumberInt = Integer.parseInt(otherTrackNumber);
            return Integer.compare(thisTrackNumberInt, otherTrackNumberInt);
        }

        return ObjectUtils.compare(thisTrackNumber, otherTrackNumber);
    }

    private int compareFileNames(String thisFileName, String otherFileName) {
        return new File(thisFileName).getName().compareTo(new File(otherFileName).getName());
    }

    private boolean isEmpty(String value) {
        return value == null || value.trim().isEmpty();
    }

    private boolean isInteger(String value) {
        if (isEmpty(value)) {
            return false;
        }

        try {
            Integer.parseInt(value);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AudioWrapper that = (AudioWrapper) o;

        if (hasCover != that.hasCover) return false;
        if (!Objects.equals(id, that.id)) return false;
        if (!Objects.equals(filePath, that.filePath)) return false;
        if (!Objects.equals(album, that.album)) return false;
        return Objects.equals(trackNumber, that.trackNumber);
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (filePath != null ? filePath.hashCode() : 0);
        result = 31 * result + (hasCover ? 1 : 0);
        result = 31 * result + (album != null ? album.hashCode() : 0);
        result = 31 * result + (trackNumber != null ? trackNumber.hashCode() : 0);
        return result;
    }

    public String getIdentifier() {
        return "%s_%s".formatted(id, filePath.hashCode());
    }

    @Override
    public String toString() {
        return filePath;
    }

    public Optional<Image> loadImage() {
        return AudioUtil.getCover(getFilePath());
    }
}
