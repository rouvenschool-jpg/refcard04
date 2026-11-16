package ch.bbw.architecturerefcard04;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CommonPrefix;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Speichert Dateien in einem privaten AWS S3 Bucket statt im Ordner ./uploads/.
 * Downloads laufen über zeitlich begrenzte Presigned URLs.
 */
@Service
public class S3StorageService {

    /** Gültigkeit der Download-Links (endet spätestens mit den AWS-Credentials). */
    private static final Duration URL_VALIDITY = Duration.ofMinutes(10);

    private final S3Client s3;
    private final S3Presigner presigner;
    private final String bucket;

    public S3StorageService(S3Client s3,
                            S3Presigner presigner,
                            @Value("${aws.s3.bucket}") String bucket) {
        this.s3 = s3;
        this.presigner = presigner;
        this.bucket = bucket;
    }

    /** Lädt eine Datei hoch. Der Dateiname wird als Key verwendet. */
    public void storeFile(MultipartFile file) {
        String key = StringUtils.cleanPath(
                Objects.requireNonNull(file.getOriginalFilename()));

        // contentType: Bild im Browser anzeigen statt herunterladen
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(file.getContentType())
                .build();

        try (InputStream in = file.getInputStream()) {
            s3.putObject(request,
                    RequestBody.fromInputStream(in, file.getSize()));
        } catch (IOException e) {
            throw new RuntimeException(
                    "Upload fehlgeschlagen: " + key, e);
        }
    }

    /**
     * Liefert den Inhalt eines Ordners (nur eine Ebene): Unterordner und
     * Dateien mit Download-Link. S3 kennt keine echten Ordner, ein Ordner
     * ist ein gemeinsames Key-Prefix wie "bilder/". Mit dem Delimiter "/"
     * fasst S3 alles unterhalb eines Unterordners zu einem Eintrag zusammen.
     *
     * @param prefix Pfad des Ordners ("" für die oberste Ebene, sonst mit
     *               abschliessendem "/")
     */
    public StorageListing list(String prefix) {
        ListObjectsV2Request request = ListObjectsV2Request.builder()
                .bucket(bucket)
                .prefix(prefix)
                .delimiter("/")
                .build();

        List<StoredFolder> folders = new ArrayList<>();
        List<StoredFile> files = new ArrayList<>();

        // Der Paginator holt bei mehr als 1000 Einträgen automatisch weitere Seiten.
        for (ListObjectsV2Response page : s3.listObjectsV2Paginator(request)) {
            for (CommonPrefix commonPrefix : page.commonPrefixes()) {
                String path = commonPrefix.prefix();
                folders.add(new StoredFolder(
                        path.substring(prefix.length(), path.length() - 1), path));
            }
            for (S3Object object : page.contents()) {
                String key = object.key();
                // Platzhalter-Objekt, das die AWS-Konsole für leere Ordner anlegt ("bilder/")
                if (key.endsWith("/")) {
                    continue;
                }
                files.add(new StoredFile(key.substring(prefix.length()), getFileUrl(key)));
            }
        }
        return new StorageListing(folders, files);
    }

    /**
     * Liefert eine zeitlich begrenzte Download-URL für ein Objekt.
     * Wird lokal berechnet (kein Netzwerkzugriff), der Bucket bleibt privat.
     */
    public String getFileUrl(String key) {
        GetObjectPresignRequest request = GetObjectPresignRequest.builder()
                .signatureDuration(URL_VALIDITY)
                .getObjectRequest(GetObjectRequest.builder()
                        .bucket(bucket)
                        .key(key)
                        .build())
                .build();
        return presigner.presignGetObject(request).url().toString();
    }

    /** Löscht ein Objekt aus dem Bucket. */
    public void deleteFile(String key) {
        s3.deleteObject(DeleteObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build());
    }
}
