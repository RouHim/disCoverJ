package de.itlobby.discoverj.searchservices;

import de.itlobby.discoverj.models.AudioWrapper;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.util.List;

import static de.itlobby.discoverj.searchservices.DeezerServiceTest.getAudioFile;
import static org.assertj.core.api.Assertions.assertThat;

class DiscogsServiceTest {

    @Test
    void searchCover() {
        // GIVEN is a audio wrapper
        AudioWrapper audioWrapper = new AudioWrapper(1, getAudioFile("test.mp3"));

        // WHEN I search for a cover
        List<BufferedImage> bufferedImages = new DiscogsService().searchCover(audioWrapper);

        // THEN
        assertThat(bufferedImages).hasSize(5);
    }
}