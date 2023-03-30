package de.itlobby.discoverj.searchengines;

import de.itlobby.discoverj.mixcd.MixCd;
import de.itlobby.discoverj.models.AudioWrapper;
import de.itlobby.discoverj.models.SearchTagWrapper;
import de.itlobby.discoverj.services.SearchModelQueryService;
import de.itlobby.discoverj.settings.Settings;
import de.itlobby.discoverj.util.AudioUtil;
import de.itlobby.discoverj.util.ImageUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static de.itlobby.discoverj.util.WSUtil.getJsonFromUrl;
import static java.text.MessageFormat.format;

public class MusicbrainzCoverSearchEngine implements CoverSearchEngine {
    //mbid rausfinden / alle mit 95+ wahrscheinlichkeit
    //http://musicbrainz.org/ws/2/release?query=release:%22meteora%22%20AND%20artist:%22linkin%20park%22&fmt=json

    //evtl anz der cover zwischenprüfen:
    //http://musicbrainz.org/ws/2/release/c2a975bc-90cc-414e-9415-17bffdbc191c?fmt=json

    //cover liste hier erhalten:
    //coverartarchive.org/release/mbid
    //https://coverartarchive.org/release/9c2d7cce-7c3b-48a0-90ec-456df13f5529

    private static final Logger log = LogManager.getLogger(MusicbrainzCoverSearchEngine.class);

    @Override
    public List<BufferedImage> search(AudioWrapper audioWrapper) {
        Optional<String> musicbrainzReleaseId = AudioUtil.getMusicbrainzReleaseId(audioWrapper);

        if (musicbrainzReleaseId.isPresent()) {
            return getCoversById(musicbrainzReleaseId.get()).toList();
        }

        String searchQuery = buildSearchQuery(audioWrapper);
        if (searchQuery == null) {
            log.info("{} fills too few requirements for musicbrainz search", audioWrapper.getFileName());
            return Collections.emptyList();
        }

        Optional<JSONObject> jsonFromUrl = getJsonFromUrl(format(
                "https://musicbrainz.org/ws/2/release?query={0}&fmt=json",
                searchQuery)
        );
        if (jsonFromUrl.isEmpty()) {
            return Collections.emptyList();
        }

        return jsonFromUrl.get()
                // search for all releases with a score > 95
                .getJSONArray("releases").toList().stream()
                .map(result -> new JSONObject((Map) result))
                .filter(entry -> entry.getInt("score") >= 95)
                .map(entry -> entry.getString("id"))
                // Check if any cover exists
                .map(id -> getJsonFromUrl(format("http://musicbrainz.org/ws/2/release/{0}?fmt=json", id)))
                .flatMap(Optional::stream)
                .filter(entry -> entry.getJSONObject("cover-art-archive").getInt("count") > 0)
                // find their front cover urls and download them
                .flatMap(entry -> getCoversById(entry.getString("id")))
                .toList();
    }

    private Stream<BufferedImage> getCoversById(String id) {
        Optional<JSONObject> jsonFromUrl = getJsonFromUrl(format("https://coverartarchive.org/release/{0}", id));

        if (jsonFromUrl.isEmpty()) {
            return Stream.empty();
        }

        return jsonFromUrl.get()
                .getJSONArray("images").toList().parallelStream()
                .map(image -> new JSONObject((Map) image))
                .filter(image -> image.getBoolean("front"))
                .map(image -> image.getString("image"))
                // download the cover
                .map(ImageUtil::readRGBImageFromUrl)
                .flatMap(Optional::stream)
                .filter(CoverSearchEngine::reachesMinRequiredCoverSize);
    }

    private String buildSearchQuery(AudioWrapper audioWrapper) {
        SearchTagWrapper searchTag = SearchModelQueryService.createSearchModel(audioWrapper);
        searchTag.escapeFields();

        boolean primarySingleCover = Settings.getInstance().getConfig().isPrimarySingleCover();
        boolean isMixCD = MixCd.isMixCd(audioWrapper.getParentFilePath());

        boolean hasArtist = searchTag.hasArtist();
        boolean hasTitle = searchTag.hasTitle();
        boolean hasAlbum = searchTag.hasAlbum();

        String query = null;
        if (primarySingleCover) {
            if (hasArtist && hasTitle) {
                query = escape(format("release:\"{0}\" AND artist:\"{1}\"", searchTag.getTitle(), searchTag.getArtist()));
            } else if (searchTag.hasFileName()) {
                query = escape(format("release:\"{0}\"", searchTag.getFileName()));
            }
        } else {
            if (isMixCD && hasAlbum) {
                query = escape(format("release:{0}", searchTag.getAlbum()));
            } else if (!isMixCD && hasAlbum && hasArtist) {
                query = escape(format("release:\"{0}\" AND artist:\"{1}\"", searchTag.getAlbum(), searchTag.getArtist()));
            } else if (hasArtist && hasTitle) {
                query = escape(format("release:\"{0}\" AND artist:\"{1}\"", searchTag.getTitle(), searchTag.getArtist()));
            } else if (searchTag.hasFileName()) {
                query = escape(format("release:\"{0}\"", searchTag.getFileName()));
            }
        }

        return query;
    }

    private String escape(String s) {
        return s.replace(" ", "%20");
    }
}