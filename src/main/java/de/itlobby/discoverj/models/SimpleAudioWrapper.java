package de.itlobby.discoverj.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import de.itlobby.discoverj.util.AudioUtil;
import javafx.scene.image.Image;

import java.util.Objects;
import java.util.Optional;

public class SimpleAudioWrapper {
    private Integer id;
    private String path;
    private String displayValue;
    private boolean hasCover;
    private boolean readOnly;

    public SimpleAudioWrapper() {
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public boolean isHasCover() {
        return hasCover;
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

    public String getDisplayValue() {
        return displayValue;
    }

    public void setDisplayValue(String displayValue) {
        this.displayValue = displayValue;
    }

    public boolean isReadOnly() {
        return readOnly;
    }

    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
    }

    @JsonIgnore
    public Optional<Image> getImage() {
        return hasCover
                ? AudioUtil.getCover(new AudioWrapper(this).getAudioFile())
                : Optional.empty();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        SimpleAudioWrapper that = (SimpleAudioWrapper) o;

        if (!Objects.equals(id, that.id)) {
            return false;
        }
        return Objects.equals(path, that.path);
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (path != null ? path.hashCode() : 0);
        return result;
    }
}
