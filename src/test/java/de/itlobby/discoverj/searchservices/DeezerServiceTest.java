package de.itlobby.discoverj.searchservices;

import de.itlobby.discoverj.models.AudioWrapper;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;


@Execution(ExecutionMode.CONCURRENT)
class DeezerServiceTest {

    @Test
    void searchCover() {
        // GIVEN is an audio wrapper
        AudioWrapper audioWrapper = new AudioWrapper(1, getAudioFile("test-files/test.mp3"));

        // WHEN I search for a cover
        List<BufferedImage> bufferedImages = new DeezerService().searchCover(audioWrapper);

        // THEN I get a list of buffered images
        assertThat(bufferedImages).hasSizeGreaterThan(0);
    }

    public static AudioFile getAudioFile(String name) {
        AudioFile audioFile = null;
        try {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            File file = new File(classLoader.getResource(name).getFile());
            audioFile = AudioFileIO.read(file);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return audioFile;
    }
}