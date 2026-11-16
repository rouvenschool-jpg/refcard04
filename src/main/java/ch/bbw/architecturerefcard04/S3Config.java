package ch.bbw.architecturerefcard04;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

/**
 * Stellt S3-Client und S3-Presigner als Spring Beans bereit.
 * Die Zugangsdaten liest das SDK selbst (Umgebungsvariablen
 * AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY, AWS_SESSION_TOKEN
 * oder ~/.aws/credentials).
 */
@Configuration
public class S3Config {

    @Bean
    public S3Client s3Client(@Value("${aws.region}") String region) {
        return S3Client.builder()
                .region(Region.of(region))
                .build();
    }

    /** Erzeugt zeitlich begrenzte Download-Links für den privaten Bucket. */
    @Bean
    public S3Presigner s3Presigner(@Value("${aws.region}") String region) {
        return S3Presigner.builder()
                .region(Region.of(region))
                .build();
    }
}
