package de.itlobby.discoverj.models;

import de.itlobby.discoverj.util.AudioUtil;
import org.apache.commons.lang3.ObjectUtils;
import org.jaudiotagger.audio.AudioFile;

import java.io.File;
import java.util.Objects;

import static de.itlobby.discoverj.util.StringUtil.isInteger;
import static org.apache.commons.lang3.StringUtils.isEmpty;

public class AudioWrapper implements Comparable<AudioWrapper> {
    private AudioFile audioFile;
    private boolean hasCover;
    private Integer id;

    public AudioWrapper() {
    }

    public AudioWrapper(FlatAudioWrapper simpleWrapper) {
        audioFile = AudioUtil.readAudioFile(simpleWrapper.getPath());
        hasCover = simpleWrapper.isHasCover();
        id = simpleWrapper.getId();
    }

    public AudioFile getAudioFile() {
        return audioFile;
    }

    public void setAudioFile(AudioFile audioFile) {
        this.audioFile = audioFile;
    }

    public boolean hasCover() {
        return hasCover;
    }

    public boolean hasNoCover() {
        return !hasCover;
    }

    public void setHasCover(boolean hasCover) {
        this.hasCover = hasCover;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    @Override
    public int compareTo(AudioWrapper other) {
        if (other == null) {
            return 1;
        }

        File file = getAudioFile().getFile();
        File file2 = other.getAudioFile().getFile();

        String folder = file.getParentFile().getAbsolutePath();
        String folder2 = file2.getParentFile().getAbsolutePath();
        int folderRes;

        folderRes = folder.compareTo(folder2);

        if (folderRes == 0) {
            String album = AudioUtil.getAlbum(this.getAudioFile());
            String album2 = AudioUtil.getAlbum(other.getAudioFile());

            int albumRes = ObjectUtils.compare(album, album2);

            if (albumRes == 0) {
                String trackNumber = AudioUtil.getTrackNumber(getAudioFile());
                String otherTrackNumber = AudioUtil.getTrackNumber(other.getAudioFile());

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
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AudioWrapper that = (AudioWrapper) o;
        return Objects.equals(hasCover, that.hasCover) &&
                Objects.equals(audioFile, that.audioFile) &&
                Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(audioFile, hasCover, id);
    }

    public String getFileName() {
        return getFile().getName();
    }

    public File getFile() {
        return getAudioFile().getFile();
    }

    public String getIdentifier() {
        return String.format("%s%s%s", id, audioFile.getFile().getName().hashCode(), audioFile.getAudioHeader().getTrackLength());
    }

    @Override
    public String toString() {
        return audioFile.getFile().getAbsolutePath();
    }
}