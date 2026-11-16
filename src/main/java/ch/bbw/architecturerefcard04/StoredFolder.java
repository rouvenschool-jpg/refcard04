package ch.bbw.architecturerefcard04;

/**
 * Ein "Ordner" im Bucket (gemeinsames Key-Prefix): Anzeigename und
 * vollständiger Pfad inkl. abschliessendem "/".
 */
public class StoredFolder {

    private final String name;
    private final String path;

    public StoredFolder(String name, String path) {
        this.name = name;
        this.path = path;
    }

    public String getName() {
        return name;
    }

    public String getPath() {
        return path;
    }
}
