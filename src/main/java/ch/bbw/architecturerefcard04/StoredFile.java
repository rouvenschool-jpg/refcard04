package ch.bbw.architecturerefcard04;

/**
 * Eine Datei im Bucket: Name (S3-Key) und zeitlich begrenzter Download-Link.
 */
public class StoredFile {

    private final String name;
    private final String url;

    public StoredFile(String name, String url) {
        this.name = name;
        this.url = url;
    }

    public String getName() {
        return name;
    }

    public String getUrl() {
        return url;
    }
}
