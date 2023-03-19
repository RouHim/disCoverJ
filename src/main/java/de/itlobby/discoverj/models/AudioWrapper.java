package de.itlobby.discoverj.models;

import de.itlobby.discoverj.util.AudioUtil;
import de.itlobby.discoverj.util.StringUtil;
import javafx.scene.image.Image;
import org.apache.commons.lang3.ObjectUtils;
import org.jaudiotagger.audio.AudioFile;

import java.io.File;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static de.itlobby.discoverj.util.StringUtil.isInteger;
import static org.apache.commons.lang3.StringUtils.isEmpty;

public class AudioWrapper implements Comparable<AudioWrapper> {
    private final Integer id;
    private final String filePath;
    private boolean hasCover;
    private final String artist;
    private final List<String> artists;
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

    public AudioWrapper(Integer id, AudioFile audioFile) {
        this.id = id;
        this.filePath = audioFile.getFile().getAbsolutePath();
        this.fileName = audioFile.getFile().getName();
        this.parentFilePath = audioFile.getFile().getParentFile().getAbsolutePath();
        this.hasCover = AudioUtil.haveCover(audioFile);
        this.artist = AudioUtil.getArtist(audioFile);
        this.artists = AudioUtil.getArtists(audioFile);
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

    public List<String> getArtists() {
        return artists;
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

        File file = new File(this.filePath);
        File file2 = new File(other.filePath);

        String folder = this.getParentFilePath();
        String folder2 = other.getParentFilePath();
        int folderRes;

        folderRes = folder.compareTo(folder2);

        if (folderRes == 0) {

            int albumRes = ObjectUtils.compare(this.album, other.album);

            if (albumRes == 0) {
                String otherTrackNumber = other.trackNumber;

                if (isEmpty(trackNumber) && isEmpty(otherTrackNumber)) {
                    return ObjectUtils.compare(file.getName(), file2.getName());
                } else {
                    int trackRes;
                    if (isInteger(trackNumber) && isInteger(otherTrackNumber)) {
                        int trackNumberInt = Integer.parseInt(trackNumber);
                        int trackNumberInt2 = Integer.parseInt(otherTrackNumber);

                        trackRes = ObjectUtils.compare(trackNumberInt, trackNumberInt2);
                    } else {
                        trackRes = ObjectUtils.compare(trackNumber, otherTrackNumber);
                    }

                    return trackRes;
                }
            } else {
                return albumRes;
            }
        } else {
            return folderRes;
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