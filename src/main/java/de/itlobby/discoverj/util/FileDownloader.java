package de.itlobby.discoverj.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class FileDownloader implements Runnable {
    private static final Logger log = LogManager.getLogger(FileDownloader.class);

    private final String fileURL;
    private final File saveDir;

    private double downloadedBytes;
    private int contentLength;
    private File targetFile;

    public FileDownloader(String fileURL, File saveDir) {
        this.fileURL = fileURL;
        this.saveDir = saveDir;
    }

    @Override
    public void run() {
        downloadFile();
    }

    public boolean validateURL() {
        boolean isOk = false;

        try {
            URL url = new URL(fileURL);
            HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
            int responseCode = httpConn.getResponseCode();
            isOk = responseCode == HttpURLConnection.HTTP_OK;
        } catch (IOException e) {
            log.debug(e.getMessage(), e);
        }

        return isOk;
    }

    private void downloadFile() {
        try {
            URL url = new URL(fileURL);

            HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
            int responseCode = httpConn.getResponseCode();

            // always check HTTP response code first
            if (responseCode == HttpURLConnection.HTTP_OK) {
                String fileName = "";
                String disposition = httpConn.getHeaderField("Content-Disposition");
                String contentType = httpConn.getContentType();
                contentLength = httpConn.getContentLength();

                if (disposition != null) {
                    // extracts file name from header field
                    int index = disposition.indexOf("filename=");
                    if (index > 0) {
                        fileName = disposition.substring(index + 10,
                                disposition.length() - 1
                        );
                    }
                } else {
                    // extracts file name from URL
                    fileName = fileURL.substring(fileURL.lastIndexOf("/") + 1
                    );
                }

                log.debug("Content-Type = {}", contentType);
                log.debug("Content-Disposition = {}", disposition);
                log.debug("Content-Length = {}", contentLength);
                log.debug("fileName = {}", fileName);

                // opens input stream from the HTTP connection
                readFromConnection(httpConn, fileName);
            } else {
                log.debug("No file to download. Server replied HTTP code: {}", responseCode);
            }

            httpConn.disconnect();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    private void readFromConnection(HttpURLConnection httpConn, String fileName) throws IOException {
        InputStream inputStream = httpConn.getInputStream();
        targetFile = new File(saveDir, fileName);

        // opens an output stream to save into file
        try (FileOutputStream outputStream = new FileOutputStream(targetFile)) {
            int bytesRead;
            byte[] buffer = new byte[4096];
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                downloadedBytes += bytesRead;
                outputStream.write(buffer, 0, bytesRead);
            }

            inputStream.close();
            log.debug("File downloaded");
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }

    public double getCurrentProgress() {
        double value = downloadedBytes / (double) contentLength;

        if (value >= 0.0 && value <= 1.0) {
            return value;
        }

        return 0.0;
    }

    public File getTargetFile() {
        return targetFile;
    }
}