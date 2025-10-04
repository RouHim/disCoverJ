package de.itlobby.discoverj.ui.viewtask;

import de.itlobby.discoverj.util.AudioUtil;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.io.FileUtils;

public class PreCountViewTask extends ViewTask<List<String>> {

    private final File[] musicObjects;
    private List<String> audiosToLoad;

    public PreCountViewTask(File[] musicObjects) {
        this.musicObjects = musicObjects;
    }

    @Override
    public void work() {
        audiosToLoad = new ArrayList<>();

        for (File musicObject : musicObjects) {
            if (isCancelled()) {
                break;
            }

            countFile(musicObject);
        }

        setResult(audiosToLoad);
    }

    private void countFile(File rootFile) {
        if (isCancelled()) {
            return;
        }

        if (!rootFile.isDirectory()) {
            if (AudioUtil.isAudioFile(rootFile)) {
                audiosToLoad.add(rootFile.getAbsolutePath());
            }
            return;
        }

        for (File fileObj : FileUtils.listFiles(rootFile, null, true)) {
            if (fileObj.isDirectory()) {
                countFile(fileObj);
            } else {
                if (AudioUtil.isAudioFile(fileObj)) {
                    audiosToLoad.add(fileObj.getAbsolutePath());
                }
            }
        }
    }
}
