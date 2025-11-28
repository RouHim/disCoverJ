package de.itlobby.discoverj.searchengines;

import static org.assertj.core.api.Assertions.assertThat;

import de.itlobby.discoverj.models.AudioWrapper;
import de.itlobby.discoverj.models.ImageFile;
import java.io.File;
import java.util.List;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

@Execution(ExecutionMode.CONCURRENT)
class DeezerServiceTest {

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

  @Test
  void searchCover() {
    // GIVEN is an audio wrapper
    AudioWrapper audioWrapper = new AudioWrapper(
      1,
      getAudioFile("test-files/test.mp3")
    );

    // WHEN I search for a cover
    List<ImageFile> coverImages = new DeezerCoverSearchEngine().search(
      audioWrapper
    );

    // THEN I get a list of buffered images
    assertThat(coverImages).hasSizeGreaterThan(0);
  }
}
