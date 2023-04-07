package de.itlobby.discoverj.settings;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import de.itlobby.discoverj.models.Language;
import de.itlobby.discoverj.models.SearchEngine;
import de.itlobby.discoverj.util.ConfigUtil;

import java.io.Serializable;
import java.util.List;
import java.util.Locale;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AppConfig implements Serializable {
    private boolean overwriteCover = false;
    private int minCoverSize = 250;
    private int maxCoverSize = 2500;
    private List<SearchEngine> searchEngineList = ConfigUtil.getDefaultSearchEngineList();
    private Language language = Language.fromLocale(Locale.getDefault());
    private boolean proxyEnabled = false;
    private String proxyUrl = "";
    private String proxyPort = "";
    private String proxyUser = "";
    private String proxyPassword = "";
    /**
     * Search timeout in seconds
     */
    private int searchTimeout = 60;
    private boolean overwriteOnlyHigher = false;
    private String localAdditionalFolderPath = "";
    private String localNamePattern = "%artist% - %title%";
    private boolean generalManualImageSelection = false;
    private boolean generalAutoLastAudio = true;
    private String lastFolderPath = "";
    private boolean discogsUseYear = false;
    private boolean discogsUseCountry = false;
    private String discogsCountry = "";
    private boolean primarySingleCover = false;
    private String googleSearchPattern = "%auto% cover";
    private String gracenoteClientID = "";
    private boolean localScanAudioFiles = false;
    private boolean localMatchAlbum = false;
    private boolean localMatchAlbumArtist = false;
    private boolean localMatchYear = false;
    private String searxCustomInstance = "https://searx.netzspielplatz.de";
    private boolean searxCustomInstanceActive = false;

    public AppConfig() {
        // empty constructor for jackson
    }

    public boolean isOverwriteCover() {
        return overwriteCover;
    }

    public void setOverwriteCover(boolean overwriteCover) {
        this.overwriteCover = overwriteCover;
    }

    public int getMinCoverSize() {
        return minCoverSize;
    }

    public void setMinCoverSize(int minCoverSize) {
        this.minCoverSize = minCoverSize;
    }

    public int getMaxCoverSize() {
        return maxCoverSize;
    }

    public void setMaxCoverSize(int maxCoverSize) {
        this.maxCoverSize = maxCoverSize;
    }

    public List<SearchEngine> getSearchEngineList() {
        return searchEngineList;
    }

    public void setSearchEngineList(List<SearchEngine> searchEngineList) {
        this.searchEngineList = searchEngineList;
    }

    public Language getLanguage() {
        return language;
    }

    public void setLanguage(Language language) {
        this.language = language;
    }

    public boolean isProxyEnabled() {
        return proxyEnabled;
    }

    public void setProxyEnabled(boolean proxyEnabled) {
        this.proxyEnabled = proxyEnabled;
    }

    public String getProxyUrl() {
        return proxyUrl;
    }

    public void setProxyUrl(String proxyUrl) {
        this.proxyUrl = proxyUrl;
    }

    public String getProxyPort() {
        return proxyPort;
    }

    public void setProxyPort(String proxyPort) {
        this.proxyPort = proxyPort;
    }

    public String getProxyUser() {
        return proxyUser;
    }

    public void setProxyUser(String proxyUser) {
        this.proxyUser = proxyUser;
    }

    public String getProxyPassword() {
        return proxyPassword;
    }

    public void setProxyPassword(String proxyPassword) {
        this.proxyPassword = proxyPassword;
    }

    public int getSearchTimeout() {
        return searchTimeout;
    }

    public void setSearchTimeout(int searchTimeout) {
        this.searchTimeout = searchTimeout;
    }

    public boolean isOverwriteOnlyHigher() {
        return overwriteOnlyHigher;
    }

    public void setOverwriteOnlyHigher(boolean overwriteOnlyHigher) {
        this.overwriteOnlyHigher = overwriteOnlyHigher;
    }

    public String getLocalAdditionalFolderPath() {
        return localAdditionalFolderPath;
    }

    public void setLocalAdditionalFolderPath(String localAdditionalFolderPath) {
        this.localAdditionalFolderPath = localAdditionalFolderPath;
    }

    public String getLocalNamePattern() {
        return localNamePattern;
    }

    public void setLocalNamePattern(String localNamePattern) {
        this.localNamePattern = localNamePattern;
    }

    public boolean isGeneralManualImageSelection() {
        return generalManualImageSelection;
    }

    public void setGeneralManualImageSelection(boolean generalManualImageSelection) {
        this.generalManualImageSelection = generalManualImageSelection;
    }

    public boolean isGeneralAutoLastAudio() {
        return generalAutoLastAudio;
    }

    public void setGeneralAutoLastAudio(boolean generalAutoLastAudio) {
        this.generalAutoLastAudio = generalAutoLastAudio;
    }

    public String getLastFolderPath() {
        return lastFolderPath;
    }

    public void setLastFolderPath(String lastFolderPath) {
        this.lastFolderPath = lastFolderPath;
    }

    public boolean isDiscogsUseYear() {
        return discogsUseYear;
    }

    public void setDiscogsUseYear(boolean discogsUseYear) {
        this.discogsUseYear = discogsUseYear;
    }

    public boolean isDiscogsUseCountry() {
        return discogsUseCountry;
    }

    public void setDiscogsUseCountry(boolean discogsUseCountry) {
        this.discogsUseCountry = discogsUseCountry;
    }

    public String getDiscogsCountry() {
        return discogsCountry;
    }

    public void setDiscogsCountry(String discogsCountry) {
        this.discogsCountry = discogsCountry;
    }

    public boolean isPrimarySingleCover() {
        return primarySingleCover;
    }

    public void setPrimarySingleCover(boolean primarySingleCover) {
        this.primarySingleCover = primarySingleCover;
    }

    public String getGoogleSearchPattern() {
        return googleSearchPattern;
    }

    public void setGoogleSearchPattern(String googleSearchPattern) {
        this.googleSearchPattern = googleSearchPattern;
    }

    public String getGracenoteClientID() {
        return gracenoteClientID;
    }

    public void setGracenoteClientID(String gracenoteClientID) {
        this.gracenoteClientID = gracenoteClientID;
    }

    public boolean isLocalScanAudioFiles() {
        return localScanAudioFiles;
    }

    public void setLocalScanAudioFiles(boolean localScanAudioFiles) {
        this.localScanAudioFiles = localScanAudioFiles;
    }

    public boolean isLocalMatchAlbum() {
        return localMatchAlbum;
    }

    public void setLocalMatchAlbum(boolean localMatchAlbum) {
        this.localMatchAlbum = localMatchAlbum;
    }

    public boolean isLocalMatchAlbumArtist() {
        return localMatchAlbumArtist;
    }

    public void setLocalMatchAlbumArtist(boolean localMatchAlbumArtist) {
        this.localMatchAlbumArtist = localMatchAlbumArtist;
    }

    public boolean isLocalMatchYear() {
        return localMatchYear;
    }

    public void setLocalMatchYear(boolean localMatchYear) {
        this.localMatchYear = localMatchYear;
    }

    public String getSearxCustomInstance() {
        return searxCustomInstance;
    }

    public void setSearxCustomInstance(String searxCustomInstance) {
        this.searxCustomInstance = searxCustomInstance;
    }

    public boolean isSearxCustomInstanceActive() {
        return searxCustomInstanceActive;
    }

    public void setSearxCustomInstanceActive(boolean searxCustomInstanceActive) {
        this.searxCustomInstanceActive = searxCustomInstanceActive;
    }
}