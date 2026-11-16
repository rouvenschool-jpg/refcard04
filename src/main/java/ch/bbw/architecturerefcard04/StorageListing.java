package ch.bbw.architecturerefcard04;

import java.util.Collections;
import java.util.List;

/**
 * Inhalt eines Ordners: Unterordner und Dateien (nur eine Ebene).
 */
public class StorageListing {

    private final List<StoredFolder> folders;
    private final List<StoredFile> files;

    public StorageListing(List<StoredFolder> folders, List<StoredFile> files) {
        this.folders = folders;
        this.files = files;
    }

    public static StorageListing empty() {
        return new StorageListing(Collections.<StoredFolder>emptyList(),
                Collections.<StoredFile>emptyList());
    }

    public List<StoredFolder> getFolders() {
        return folders;
    }

    public List<StoredFile> getFiles() {
        return files;
    }
}
