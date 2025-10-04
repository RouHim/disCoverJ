package de.itlobby.discoverj.util;

import de.itlobby.discoverj.models.ImageFile;
import java.util.List;
import java.util.concurrent.Future;

public class SearchEngineFuture {

    private Future<List<ImageFile>> future;
    private String name;

    public SearchEngineFuture(Future<List<ImageFile>> future, String name) {
        this.future = future;
        this.name = name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public Future<List<ImageFile>> getFuture() {
        return future;
    }

    public void setFuture(Future<List<ImageFile>> future) {
        this.future = future;
    }
}
