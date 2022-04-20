package de.itlobby.discoverj.util;

import de.itlobby.discoverj.models.AudioWrapper;
import de.itlobby.discoverj.models.FlatAudioWrapper;
import de.itlobby.discoverj.services.DataService;
import de.itlobby.discoverj.services.LightBoxService;
import de.itlobby.discoverj.ui.core.ServiceLocator;
import de.itlobby.discoverj.util.helper.ImageCache;
import javafx.application.Platform;
import javafx.scene.image.Image;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.SupportedFileFormat;
import org.jaudiotagger.audio.exceptions.CannotWriteException;
import org.jaudiotagger.audio.mp3.MP3File;
import org.jaudiotagger.tag.FieldDataInvalidException;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.id3.AbstractTagFrame;
import org.jaudiotagger.tag.id3.AbstractTagFrameBody;
import org.jaudiotagger.tag.id3.ID3v1Tag;
import org.jaudiotagger.tag.id3.ID3v23Tag;
import org.jaudiotagger.tag.images.StandardArtwork;
import org.jaudiotagger.tag.reference.PictureTypes;

import java.awt.image.BufferedImage;
import java.io.File;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static de.itlobby.discoverj.searchservice.DiscogsService.DISCOGS_RELEASE_ID;
import static de.itlobby.discoverj.util.StringUtil.ARTISTS_SEPARATOR_KEYWORDS;
import static org.jaudiotagger.tag.FieldKey.MUSICBRAINZ_RELEASEID;

public class AudioUtil {

    public static final String[] VALID_IMAGE_FILE_EXTENSION = {"jpg", "jpeg", "bmp", "png", "gif"};
    public static final String[] VALID_AUDIO_FILE_EXTENSION =
            Arrays.stream(SupportedFileFormat.values())
                    .map(SupportedFileFormat::getFilesuffix)
                    .toArray(String[]::new);
    public static final List<String> VALID_AUDIO_FILE_EXTENSION_LIST =
            Arrays.stream(VALID_AUDIO_FILE_EXTENSION).toList();
    private static final Logger log = LogManager.getLogger(AudioUtil.class);

    private AudioUtil() {
    }

    public static boolean haveCover(AudioFile audioFile) {
        Tag tag = audioFile.getTag();
        return tag != null && tag.getFirstArtwork() != null && tag.getFirstArtwork().getBinaryData() != null;
    }

    public static Optional<Image> getCover(AudioFile audioFile, int width, int height) {
        byte[] data = AudioUtil.getCoverData(audioFile);
        return ImageCache.getInstance().getImage(data, width, height);
    }

    public static Optional<Image> getCover(AudioFile audioFile) {
        return Optional.ofNullable(AudioUtil.getCoverData(audioFile))
                .flatMap(imageData -> ImageCache.getInstance().getImage(imageData));
    }

    public static Optional<BufferedImage> getCoverAsBufImg(AudioFile audioFile) {
        try {
            return Optional.ofNullable(getCoverData(audioFile))
                    .map(data -> ImageCache.getInstance().getBuffImage(data));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

        return Optional.empty();
    }

    private static byte[] getCoverData(AudioFile audioFile) {
        Tag tag = audioFile.getTag();

        if (tag != null && tag.getFirstArtwork() != null) {
            return tag.getFirstArtwork().getBinaryData();
        }

        return null;
    }

    public static String getAlbum(AudioFile audioFile) {
        try {
            return readFieldValue(audioFile, FieldKey.ALBUM);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

        return "";
    }

    public static String getAlbumArtist(AudioFile audioFile) {
        try {
            return readFieldValue(audioFile, FieldKey.ALBUM_ARTIST);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

        return "";
    }

    private static Optional<String> readMaybeFieldValue(AudioFile audioFile, FieldKey fieldKey) {
        Tag tag = audioFile.getTag();
        return tag != null && tag.hasField(fieldKey)
                ? Optional.ofNullable(tag.getFirst(fieldKey))
                : Optional.empty();
    }

    private static String readFieldValue(AudioFile audioFile, FieldKey fieldKey) {
        Tag tag = audioFile.getTag();

        return tag != null && tag.hasField(fieldKey)
                ? tag.getFirst(fieldKey)
                : "";
    }

    public static String getTitle(AudioFile audioFile) {
        try {
            return readFieldValue(audioFile, FieldKey.TITLE);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

        return "";
    }

    public static String getArtist(AudioFile audioFile) {
        try {
            return readFieldValue(audioFile, FieldKey.ARTIST);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

        return "";
    }

    public static List<String> getArtists(AudioFile audioFile) {
        String artistSeparators = ARTISTS_SEPARATOR_KEYWORDS.stream()
                .map(entry -> entry.replace("+", "\\+"))
                .collect(Collectors.joining("|"));

        return Arrays.stream(readFieldValue(audioFile, FieldKey.ARTIST)
                        .split(artistSeparators))
                .map(String::trim)
                .toList();
    }

    public static String getYear(AudioFile audioFile) {
        try {
            return readFieldValue(audioFile, FieldKey.YEAR);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

        return "";
    }

    public static String getTrackNumber(AudioFile audioFile) {
        try {
            return readFieldValue(audioFile, FieldKey.TRACK);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

        return null;
    }

    public static Optional<String> getMusicbrainzReleaseId(AudioFile audioFile) {
        return readMaybeFieldValue(audioFile, MUSICBRAINZ_RELEASEID);
    }

    public static Optional<Integer> getDiscogsReleaseId(AudioFile audioFile) {
        return readCustomFieldValue(audioFile, DISCOGS_RELEASE_ID)
                .map(Integer::valueOf);
    }

    private static Optional<String> readCustomFieldValue(AudioFile audioFile, String customField) {
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(audioFile.getTag().getFields(), Spliterator.ORDERED), false)
                .filter(field -> field instanceof AbstractTagFrame)
                .map(field -> ((AbstractTagFrame) field).getBody())
                .filter(body -> body.getBriefDescription().contains(customField))
                .findFirst()
                .map(AbstractTagFrameBody::getUserFriendlyValue);
    }

    public static void saveCoverToAudioFile(AudioFile audioFile, BufferedImage cover) {
        if (cover == null) {
            return;
        }

        try {
            writeAudioTag(audioFile, cover);
            audioFile.commit();
        } catch (CannotWriteException e) {
            showCannotWriteError(audioFile.getFile().getAbsolutePath());
        } catch (Exception e) {
            log.error("Error while processing file: {}", audioFile.getFile().getAbsolutePath(), e);
        }
    }

    private static void writeAudioTag(AudioFile audioFile, BufferedImage cover) throws FieldDataInvalidException {
        byte[] imageBytes = ImageUtil.toJpgByteArray(cover);

        convertTagIfNeeded(audioFile);

        StandardArtwork artwork = new StandardArtwork();
        artwork.setBinaryData(imageBytes);
        artwork.setHeight(cover.getHeight());
        artwork.setWidth(cover.getWidth());
        artwork.setMimeType("image/jpeg");
        artwork.setPictureType(PictureTypes.DEFAULT_ID);

        if (audioFile.getTag() == null) {
            audioFile.setTag((audioFile.createDefaultTag()));
        }

        audioFile.getTag().deleteArtworkField();
        audioFile.getTag().setField(artwork);
    }

    /**
     * Converts the audio file ID3v1Tag tag to ID3v23Tag
     *
     * @param audioFile to convert
     */
    private static void convertTagIfNeeded(AudioFile audioFile) {
        try {
            if (audioFile.getTag() instanceof ID3v1Tag) {
                MP3File mp3 = (MP3File) audioFile;
                ID3v1Tag id3v1Tag = mp3.getID3v1Tag();
                audioFile.setTag(new ID3v23Tag(id3v1Tag));
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    public static boolean hasSameFolderAndAlbumAsLast(AudioWrapper currentAudio, String lastAudioPath) {
        boolean equalFolder = false;
        boolean equalAlbum = false;

        if (currentAudio == null || lastAudioPath == null) {
            return false;
        }

        try {
            String currentFolder = currentAudio.getAudioFile().getFile().getParent();
            File lastAudio = new File(lastAudioPath);
            String lastFolder = lastAudio.getParent();

            equalFolder = currentFolder.equals(lastFolder);

            String currentAlbum = AudioUtil.getAlbum(currentAudio.getAudioFile());
            AudioFile lastAudioFile = AudioFileIO.read(lastAudio);
            String lastAlbum = AudioUtil.getAlbum(lastAudioFile);

            if (!StringUtil.isNullOrEmpty(currentAlbum) && !StringUtil.isNullOrEmpty(lastAlbum)) {
                equalAlbum = currentAlbum.equals(lastAlbum);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

        return equalFolder && equalAlbum;
    }

    public static void removeCover(FlatAudioWrapper flatWrapper) {
        String filePath = flatWrapper.getPath();
        try {
            AudioFile audioFile = AudioFileIO.read(new File(filePath));
            audioFile.getTag().deleteArtworkField();
            audioFile.commit();
            flatWrapper.setHasCover(false);
        } catch (CannotWriteException e) {
            showCannotWriteError(filePath);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    public static String buildDisplayValue(AudioWrapper audioWrapper) {
        String res = "";

        AudioFile audioFile = audioWrapper.getAudioFile();
        if (!StringUtil.isNullOrEmpty(getArtist(audioFile))) {
            res += getArtist(audioFile);

            if (!StringUtil.isNullOrEmpty(getTitle(audioFile))) {
                res += " - " + getTitle(audioFile);
            }
        } else {
            res += audioWrapper.getFileName();
        }

        if (!StringUtil.isNullOrEmpty(getAlbum(audioFile))) {
            res += "\n" + getAlbum(audioFile);
        }

        return res;
    }

    public static boolean isAudioFile(File file) {
        return VALID_AUDIO_FILE_EXTENSION_LIST
                .contains(StringUtil.getFileExtension(file).toLowerCase());
    }

    private static AudioFile readAudioFileSafe(File file) {
        try {
            return AudioFileIO.read(file.getAbsoluteFile());
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }

    public static boolean checkForMixCD(AudioWrapper audioWrapper) {
        File parentFile = audioWrapper.getFile().getParentFile();
        String absoluteParentPath = parentFile.getAbsolutePath();

        DataService dataService = ServiceLocator.get(DataService.class);

        Collection<File> audioFilesInFolder = FileUtils.listFiles(parentFile, VALID_AUDIO_FILE_EXTENSION, false);

        if (dataService.getMixCDMap().containsKey(absoluteParentPath)) {
            return dataService.checkForMixCDEntry(absoluteParentPath);
        }

        int totalSize = audioFilesInFolder.size();

        if (totalSize < 3)
            return false;

        Map<String, Long> grouped = audioFilesInFolder.parallelStream()
                .flatMap(x -> getArtists(readAudioFileSafe(x)).stream())
                .collect(Collectors.groupingBy(x -> x, Collectors.counting()));

        Map.Entry<String, Long> firstEntry = CollectionUtil.sortByValueDescAndGetFirst(grouped);
        long biggest = firstEntry.getValue();

        double artistOfTotalPercentage = (double) biggest / (double) totalSize;

        boolean isMixCD = artistOfTotalPercentage < 0.50;
        dataService.getMixCDMap().put(absoluteParentPath, isMixCD);

        log.debug("IsMixCD: {} ({})", isMixCD, artistOfTotalPercentage);
        return isMixCD;
    }

    public static String getImageResolutionString(Image cover) {
        return ImageUtil.createImageResolutionString(cover.getWidth(), cover.getHeight());
    }

    public static Optional<AudioFile> getAudioFile(String path) {
        try {
            return Optional.ofNullable(AudioFileIO.read(new File(path)));
        } catch (Exception e) {
            e.printStackTrace();
        }

        return Optional.empty();
    }

    public static void showCannotWriteError(String absolutePath) {
        Platform.runLater(() -> ServiceLocator.get(LightBoxService.class)
                .showTextDialog(
                        LanguageUtil.getString("InitialController.warning"),
                        MessageFormat.format(
                                "{0}\n\n{1}",
                                LanguageUtil.getString("SearchController.cannotWriteFile"),
                                absolutePath)
                )
        );
    }
}
