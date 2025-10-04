package de.itlobby.discoverj.models;

import java.util.Objects;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Version implements Comparable<Version> {

    private static final Logger log = LogManager.getLogger(Version.class);
    private Integer major = 0;
    private Integer minor = 0;
    private Integer revision = 0;

    public Version(String version) {
        String[] parts = version.trim().split("\\.");

        if (parts.length == 1) {
            major = Integer.parseInt(parts[0]);
        } else if (parts.length == 2) {
            major = Integer.parseInt(parts[0]);
            minor = Integer.parseInt(parts[1]);
        } else if (parts.length == 3) {
            major = Integer.parseInt(parts[0]);
            minor = Integer.parseInt(parts[1]);
            revision = Integer.parseInt(parts[2]);
        } else {
            log.error("not a valid version: {}", version);
        }
    }

    public Integer getMajor() {
        return major;
    }

    public void setMajor(Integer major) {
        this.major = major;
    }

    public Integer getMinor() {
        return minor;
    }

    public void setMinor(Integer minor) {
        this.minor = minor;
    }

    public Integer getRevision() {
        return revision;
    }

    public void setRevision(Integer revision) {
        this.revision = revision;
    }

    @Override
    public int compareTo(Version other) {
        if (other == null) {
            return 1;
        }

        if (major.compareTo(other.getMajor()) == 0) {
            if (minor.compareTo(other.getMinor()) == 0) {
                if (revision.compareTo(other.getRevision()) == 0) {
                    return 0;
                } else {
                    return revision.compareTo(other.getRevision());
                }
            } else {
                return minor.compareTo(other.getMinor());
            }
        } else {
            return major.compareTo(other.getMajor());
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Version version = (Version) o;
        return (
            Objects.equals(major, version.major) &&
            Objects.equals(minor, version.minor) &&
            Objects.equals(revision, version.revision)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(major, minor, revision);
    }

    @Override
    public String toString() {
        return major + "." + minor + "." + revision;
    }
}
