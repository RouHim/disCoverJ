package de.itlobby.discoverj.searchengines;

import de.itlobby.discoverj.models.AudioWrapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.awt.image.BufferedImage;
import java.util.List;

import static de.itlobby.discoverj.searchengines.DeezerServiceTest.getAudioFile;
import static org.assertj.core.api.Assertions.assertThat;

@Execution(ExecutionMode.CONCURRENT)
class SearxServiceTest {

    @Test
    void searchCover() {
        // GIVEN is an audio wrapper
        AudioWrapper audioWrapper = new AudioWrapper(1, getAudioFile("test-files/test.mp3"));

        // WHEN I search for a cover
        List<BufferedImage> bufferedImages = new SearxCoverSearchEngine().search(audioWrapper);

        // THEN I get a list of buffered images
        assertThat(bufferedImages).hasSizeGreaterThan(0);
    }
}