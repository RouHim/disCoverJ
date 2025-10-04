package de.itlobby.discoverj.models;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AudioWrapperTest {

    @Test
    void testCompareTo_equalAudioWrappers() {
        AudioWrapper audio1 = new AudioWrapper(
            1,
            "file1.mp3",
            false,
            "Artist",
            "Title",
            "Album",
            "1",
            "parent",
            "file1",
            1000,
            true,
            "mp3",
            "2023",
            "AlbumArtist"
        );
        AudioWrapper audio2 = new AudioWrapper(
            1,
            "file1.mp3",
            false,
            "Artist",
            "Title",
            "Album",
            "1",
            "parent",
            "file1",
            1000,
            true,
            "mp3",
            "2023",
            "AlbumArtist"
        );
        int result = audio1.compareTo(audio2);
        assertThat(result).isZero();
    }

    @Test
    void testCompareTo_differentAlbum() {
        AudioWrapper audio1 = new AudioWrapper(
            1,
            "file1.mp3",
            false,
            "Artist",
            "Title",
            "Album A",
            "1",
            "parent",
            "file1",
            1000,
            true,
            "mp3",
            "2023",
            "AlbumArtist"
        );
        AudioWrapper audio2 = new AudioWrapper(
            2,
            "file2.mp3",
            false,
            "Artist",
            "Title",
            "Album B",
            "1",
            "parent",
            "file2",
            1000,
            true,
            "mp3",
            "2023",
            "AlbumArtist"
        );
        int result = audio1.compareTo(audio2);
        assertThat(result).isNegative();
    }

    @Test
    void testCompareTo_differentTrackNumbers() {
        AudioWrapper audio1 = new AudioWrapper(
            1,
            "file1.mp3",
            false,
            "Artist",
            "Title",
            "Album",
            "2",
            "parent",
            "file1",
            1000,
            true,
            "mp3",
            "2023",
            "AlbumArtist"
        );
        AudioWrapper audio2 = new AudioWrapper(
            2,
            "file2.mp3",
            false,
            "Artist",
            "Title",
            "Album",
            "1",
            "parent",
            "file2",
            1000,
            true,
            "mp3",
            "2023",
            "AlbumArtist"
        );
        int result = audio1.compareTo(audio2);
        assertThat(result).isPositive();
    }

    @Test
    void testCompareTo_nullAudioWrapper() {
        AudioWrapper audio1 = new AudioWrapper(
            1,
            "file1.mp3",
            false,
            "Artist",
            "Title",
            "Album",
            "1",
            "parent",
            "file1",
            1000,
            true,
            "mp3",
            "2023",
            "AlbumArtist"
        );
        int result = audio1.compareTo(null);
        assertThat(result).isPositive();
    }
}
