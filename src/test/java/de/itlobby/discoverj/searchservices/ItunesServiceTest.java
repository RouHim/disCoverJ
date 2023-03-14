package de.itlobby.discoverj.searchservices;

import de.itlobby.discoverj.models.AudioWrapper;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.util.List;

import static de.itlobby.discoverj.searchservices.DeezerServiceTest.getAudioFile;
import static org.assertj.core.api.Assertions.assertThat;


class ItunesServiceTest {

    @Test
    void searchCover() {
        // GIVEN is a audio wrapper
        AudioWrapper audioWrapper = new AudioWrapper(1, getAudioFile("test.mp3"));

        // WHEN I search for a cover
        List<BufferedImage> coverImages = new ItunesService().searchCover(audioWrapper);

        // THEN
        assertThat(coverImages).hasSize(5);
    }
}